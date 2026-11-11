package com.quickmsg.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.quickmsg.app.data.QuickMessages
import com.quickmsg.app.data.WidgetConfig
import com.quickmsg.app.data.WidgetRepository
import com.quickmsg.app.service.TemplateProcessor
import com.quickmsg.app.ui.theme.QuickMsgTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val editWidgetId = intent?.getIntExtra("EDIT_WIDGET_ID", -1) ?: -1
        val repo = WidgetRepository(this)

        CoroutineScope(Dispatchers.IO).launch {
            val targetWidgetId = when {
                editWidgetId != -1 -> editWidgetId
                appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID -> appWidgetId
                else -> {
                    // Launched from pin callback — find newest unconfigured widget
                    val manager = AppWidgetManager.getInstance(this@WidgetConfigActivity)
                    val provider = android.content.ComponentName(
                        this@WidgetConfigActivity, QuickMsgWidgetReceiver::class.java
                    )
                    val allIds = manager.getAppWidgetIds(provider)
                    val unconfigured = allIds.filter { repo.getByWidgetId(it) == null }
                    unconfigured.lastOrNull() ?: run {
                        runOnUiThread { finish() }
                        return@launch
                    }
                }
            }
            val existing = repo.getByWidgetId(targetWidgetId)
            runOnUiThread {
                setContent {
                    QuickMsgTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            WidgetConfigScreen(
                                existing = existing,
                                onSave = { config -> saveWidget(config, targetWidgetId) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun saveWidget(config: WidgetConfig, targetWidgetId: Int) {
        val repo = WidgetRepository(this)
        val finalConfig = config.copy(widgetId = targetWidgetId)

        CoroutineScope(Dispatchers.IO).launch {
            repo.save(finalConfig)

            try {
                val manager = GlanceAppWidgetManager(this@WidgetConfigActivity)
                val glanceId = manager.getGlanceIdBy(targetWidgetId)
                QuickMsgWidget().update(this@WidgetConfigActivity, glanceId)
            } catch (_: Exception) {
                // Widget might not exist yet if editing from app
            }

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, targetWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetConfigScreen(
    existing: WidgetConfig?,
    onSave: (WidgetConfig) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var contactName by remember { mutableStateOf(existing?.contactName ?: "") }
    var contactPhone by remember { mutableStateOf(existing?.contactPhone ?: "") }
    var primaryMessage by remember { mutableStateOf(existing?.message ?: "") }
    val additionalMessages = remember {
        mutableStateListOf<String>().apply {
            existing?.messages?.let { addAll(it) }
        }
    }
    var selectedColor by remember { mutableStateOf(existing?.colorHex ?: "#25D366") }
    var selectedSize by remember { mutableStateOf(existing?.widgetSize ?: "medium") }
    var showPresets by remember { mutableStateOf(false) }
    var newMessage by remember { mutableStateOf("") }

    val colorOptions = listOf(
        "#25D366", "#128C7E", "#075E54", "#34B7F1",
        "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
        "#2196F3", "#FF9800", "#F44336", "#4CAF50"
    )

    val sizeOptions = listOf("small", "medium", "large")

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
                if (nameIdx >= 0) contactName = it.getString(nameIdx)
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
                                contactPhone = pc.getString(phoneIdx)
                                    .replace("[^0-9+]".toRegex(), "")
                            }
                        }
                    }
                }
                if (label.isBlank()) label = contactName
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (existing != null) "Edit Widget" else "Configure Widget",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Widget preview
        WidgetPreview(label, contactName, primaryMessage, selectedColor, selectedSize)

        Spacer(modifier = Modifier.height(24.dp))

        // ---- CONTACT SECTION ----
        Text("Contact", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Contacts, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick from Contacts")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Or enter manually:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = contactName,
            onValueChange = { contactName = it },
            label = { Text("Contact Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Phone (with country code)") },
            placeholder = { Text("+919876543210") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Widget Label") },
            placeholder = { Text("e.g., Mom, Boss") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ---- MESSAGES SECTION ----
        Text("Messages", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = primaryMessage,
            onValueChange = { primaryMessage = it },
            label = { Text("Default Message") },
            placeholder = { Text("Sent on single tap") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )

        if (TemplateProcessor.hasTemplateVars(primaryMessage)) {
            Text(
                text = "Preview: ${TemplateProcessor.previewTemplate(primaryMessage)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset messages button
        OutlinedButton(
            onClick = { showPresets = !showPresets },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showPresets) "Hide Presets" else "Browse Preset Messages")
        }

        if (showPresets) {
            Spacer(modifier = Modifier.height(8.dp))
            PresetMessageSelector(
                onSelect = { msg ->
                    if (primaryMessage.isBlank()) {
                        primaryMessage = msg
                    } else if (msg !in additionalMessages && msg != primaryMessage) {
                        additionalMessages.add(msg)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Additional messages
        if (additionalMessages.isNotEmpty()) {
            Text(
                "Additional messages (tap widget shows picker):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            additionalMessages.forEachIndexed { index, msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    IconButton(
                        onClick = { additionalMessages.removeAt(index) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add custom additional message
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                label = { Text("Add another message") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newMessage.isNotBlank()) {
                        additionalMessages.add(newMessage)
                        newMessage = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---- SIZE SECTION ----
        Text("Widget Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            sizeOptions.forEachIndexed { index, size ->
                SegmentedButton(
                    selected = selectedSize == size,
                    onClick = { selectedSize = size },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index, count = sizeOptions.size
                    )
                ) {
                    Text(size.replaceFirstChar { it.uppercase() })
                }
            }
        }

        Text(
            text = when (selectedSize) {
                "small" -> "Shows label initials only"
                "large" -> "Shows label, contact, and message preview"
                else -> "Shows label and contact name"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ---- COLOR SECTION ----
        Text("Widget Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorOptions.forEach { color ->
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(color))
                } catch (e: Exception) {
                    Color(0xFF25D366)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .then(
                            if (color == selectedColor) {
                                Modifier
                                    .border(3.dp, Color.White, CircleShape)
                                    .border(4.dp, Color.Black, CircleShape)
                            } else Modifier
                        )
                        .clickable { selectedColor = color }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (label.isNotBlank() && contactPhone.isNotBlank() && primaryMessage.isNotBlank()) {
                    onSave(
                        WidgetConfig(
                            widgetId = 0,
                            contactName = contactName,
                            contactPhone = contactPhone,
                            message = primaryMessage,
                            messages = additionalMessages.toList(),
                            label = label,
                            colorHex = selectedColor,
                            widgetSize = selectedSize
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = try {
                    Color(android.graphics.Color.parseColor(selectedColor))
                } catch (e: Exception) {
                    Color(0xFF25D366)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = label.isNotBlank() && contactPhone.isNotBlank() && primaryMessage.isNotBlank()
        ) {
            Text("Save Widget", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun WidgetPreview(
    label: String,
    contactName: String,
    message: String,
    colorHex: String,
    size: String
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF25D366)
    }

    val (w, h) = when (size) {
        "small" -> 80.dp to 80.dp
        "large" -> 200.dp to 100.dp
        else -> 160.dp to 80.dp
    }

    Box(
        modifier = Modifier
            .size(w, h)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        when (size) {
            "small" -> {
                Text(
                    text = label.ifEmpty { "?" }.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            "large" -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = label.ifEmpty { "Label" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (contactName.isNotEmpty()) {
                        Text(
                            text = contactName,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (message.isNotEmpty()) {
                        Text(
                            text = message.take(30) + if (message.length > 30) "..." else "",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label.ifEmpty { "Label" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (contactName.isNotEmpty()) {
                        Text(
                            text = contactName,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetMessageSelector(onSelect: (String) -> Unit) {
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    Column {
        QuickMessages.categories.forEach { category ->
            val messages = QuickMessages.byCategory(category)
            FilterChip(
                selected = expandedCategory == category,
                onClick = {
                    expandedCategory = if (expandedCategory == category) null else category
                },
                label = { Text(category) },
                modifier = Modifier.padding(vertical = 2.dp)
            )

            if (expandedCategory == category) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                ) {
                    messages.forEach { qm ->
                        AssistChip(
                            onClick = { onSelect(qm.text) },
                            label = {
                                Text(
                                    text = if (TemplateProcessor.hasTemplateVars(qm.text)) {
                                        "${qm.emoji} ${qm.text.substringBefore("{")}..."
                                    } else "${qm.emoji} ${qm.text}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}
