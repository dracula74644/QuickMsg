package com.quickmsg.app

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.quickmsg.app.data.PartnerPrefs
import com.quickmsg.app.data.WidgetRepository
import com.quickmsg.app.service.MessageSender
import com.quickmsg.app.service.TemplateProcessor
import com.quickmsg.app.ui.screens.HistoryScreen
import com.quickmsg.app.ui.screens.OnboardingScreen
import com.quickmsg.app.ui.screens.QuickSendScreen
import com.quickmsg.app.ui.screens.SettingsScreen
import com.quickmsg.app.ui.screens.WidgetsScreen
import com.quickmsg.app.ui.theme.QuickMsgTheme
import com.quickmsg.app.widget.QuickMsgWidgetReceiver
import com.quickmsg.app.widget.WidgetConfigActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickMsgTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val partnerPrefs = remember { PartnerPrefs(context) }
    var isSetUp by remember { mutableStateOf(partnerPrefs.isSetUp()) }
    var partnerName by remember { mutableStateOf(partnerPrefs.get()?.name ?: "") }
    var partnerPhone by remember { mutableStateOf(partnerPrefs.get()?.phone ?: "") }

    // Contact picker state
    var pendingContactName by remember { mutableStateOf("") }
    var pendingContactPhone by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
                if (nameIdx >= 0) pendingContactName = it.getString(nameIdx)
                if (idIdx >= 0) {
                    val contactId = it.getString(idIdx)
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { pc ->
                        if (pc.moveToFirst()) {
                            val phoneIdx = pc.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                            )
                            if (phoneIdx >= 0) {
                                pendingContactPhone = pc.getString(phoneIdx)
                                    .replace("[^0-9+]".toRegex(), "")
                            }
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    if (!isSetUp) {
        OnboardingScreen(
            initialName = pendingContactName,
            initialPhone = pendingContactPhone,
            onPickContact = {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onComplete = { name, phone ->
                partnerPrefs.save(name, phone)
                partnerName = name
                partnerPhone = phone
                isSetUp = true
            }
        )
    } else {
        MainApp(
            partnerName = partnerName,
            partnerPhone = partnerPhone,
            onEditPartner = { name, phone ->
                partnerPrefs.save(name, phone)
                partnerName = name
                partnerPhone = phone
            },
            onDeletePartner = {
                partnerPrefs.clear()
                partnerName = ""
                partnerPhone = ""
                isSetUp = false
            },
            onPickContact = {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            pendingContactName = pendingContactName,
            pendingContactPhone = pendingContactPhone
        )
    }
}

@Composable
fun MainApp(
    partnerName: String,
    partnerPhone: String,
    onEditPartner: (String, String) -> Unit,
    onDeletePartner: () -> Unit,
    onPickContact: () -> Unit,
    pendingContactName: String,
    pendingContactPhone: String
) {
    val context = LocalContext.current
    val repo = remember { WidgetRepository(context) }
    val widgets by repo.getAll().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem("Send", Icons.Filled.Send, Icons.Outlined.Send),
        NavItem("Widgets", Icons.Filled.Widgets, Icons.Outlined.Widgets),
        NavItem("History", Icons.Filled.History, Icons.Outlined.History),
        NavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> QuickSendScreen(
                    partnerName = partnerName,
                    partnerPhone = partnerPhone,
                    lastMessage = null,
                    lastTime = null,
                    onEditPartner = { selectedTab = 3 },
                    onDeletePartner = onDeletePartner,
                    onOpenChat = {
                        MessageSender.openChat(context, partnerPhone)
                    },
                    onSend = { message ->
                        val processed = TemplateProcessor.process(message)
                        CoroutineScope(Dispatchers.IO).launch {
                            repo.logMessage(processed, partnerPhone)
                        }
                        MessageSender.sendWhatsAppMessage(context, partnerPhone, processed)
                    }
                )
                1 -> WidgetsScreen(
                    widgets = widgets,
                    onAddWidget = { requestPinWidget(context) },
                    onEditWidget = { widgetId ->
                        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
                            putExtra("EDIT_WIDGET_ID", widgetId)
                        }
                        context.startActivity(intent)
                    },
                    onDeleteWidget = { widgetId ->
                        CoroutineScope(Dispatchers.IO).launch {
                            repo.delete(widgetId)
                        }
                    }
                )
                2 -> HistoryScreen(
                    messages = repo.sentMessageDao().getAll(),
                    onClear = {
                        CoroutineScope(Dispatchers.IO).launch {
                            repo.sentMessageDao().deleteAll()
                        }
                    }
                )
                3 -> SettingsScreen(
                    partnerName = partnerName,
                    partnerPhone = partnerPhone,
                    accessibilityEnabled = MessageSender.isAccessibilityServiceEnabled(context),
                    onEditPartner = onEditPartner,
                    onDeletePartner = onDeletePartner,
                    onPickContact = onPickContact,
                    pendingContactName = pendingContactName,
                    pendingContactPhone = pendingContactPhone,
                    onEnableAccessibility = {
                        MessageSender.openAccessibilitySettings(context)
                    }
                )
            }
        }
    }
}

fun requestPinWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val widgetProvider = ComponentName(context, QuickMsgWidgetReceiver::class.java)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val configIntent = Intent(context, WidgetConfigActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, configIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
        )
        appWidgetManager.requestPinAppWidget(widgetProvider, null, pendingIntent)
    } else {
        android.widget.Toast.makeText(
            context, "Your launcher doesn't support adding widgets from apps",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
