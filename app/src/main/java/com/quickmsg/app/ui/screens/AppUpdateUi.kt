package com.quickmsg.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quickmsg.app.service.AppUpdates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun UpdateBanner() {
    val update by AppUpdates.update.collectAsState()
    val context = LocalContext.current
    update?.let { release ->
        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("QuickMsg ${release.versionName} is available", Modifier.weight(1f))
                TextButton(onClick = { AppUpdates.download(context, release) }) { Text("Download") }
            }
        }
    }
}

@Composable
fun UpdateSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
    Text("App updates", style = MaterialTheme.typography.titleSmall)
    Text("Installed version: $version", style = MaterialTheme.typography.bodySmall)
    Text("Checks GitHub when you open the app and periodically in the background. Downloads and installation need your approval.",
        style = MaterialTheme.typography.bodySmall)
    OutlinedButton(enabled = !checking, modifier = Modifier.fillMaxWidth(), onClick = {
        checking = true
        scope.launch {
            try {
                val update = AppUpdates.check(context, force = true)
                status = if (update == null) "You’re up to date." else "Version ${update.versionName} is available. Tap Download above."
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                status = "Couldn’t check for updates. Check your connection and try again."
            } finally {
                checking = false
            }
        }
    }) { Text(if (checking) "Checking…" else "Check for updates") }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    Spacer(Modifier.height(24.dp))
}
