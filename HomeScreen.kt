package com.yourname.iprotation2.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.iprotation2.service.RotationController

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val s by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val proxyCount by vm.proxyCount.collectAsStateWithLifecycle()
    val myIp by vm.myIp.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notifGranted by remember { mutableStateOf(hasNotificationPermission(context)) }

    LifecycleResumeEffect(Unit) {
        overlayGranted = Settings.canDrawOverlays(context)
        notifGranted = hasNotificationPermission(context)
        onPauseOrDispose { }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifGranted = it
    }

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK) RotationController.start(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("IP Rotation 2.0", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Developed by NiROB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Start/Stop with countdown ring
        val fraction = if (s.running && s.total > 0) s.countdown.toFloat() / s.total else 0f
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(210.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 8.dp
            )
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(210.dp),
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 8.dp
            )
            PowerButton(running = s.running) {
                if (s.running) {
                    vm.stop()
                    RotationController.stop(context)
                } else {
                    if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val vpnIntent = android.net.VpnService.prepare(context)
                    if (vpnIntent != null) vpnLauncher.launch(vpnIntent) else RotationController.start(context)
                }
            }
        }

        Text(
            text = when {
                !s.running -> "Tap START to begin rotating"
                else -> "${s.status}  •  next change in ${s.countdown}s"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (proxyCount == 0) {
            InfoCard("No proxies yet", "Open the Proxies tab and paste your proxies to get started.")
        }

        s.error?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Current IP card
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CURRENT IP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(
                    if (s.ip.isBlank()) "—" else s.ip,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                if (s.country.isNotBlank()) {
                    Text("${s.flag} ${s.country}".trim(), style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (s.pingMs >= 0) "${s.pingMs} ms" else "— ms") },
                        leadingIcon = { Icon(Icons.Default.Speed, null, Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(s.proxyLabel.ifBlank { "No proxy" }) },
                        leadingIcon = { Icon(Icons.Default.Dns, null, Modifier.size(18.dp)) }
                    )
                }
                if (s.previousIp.isNotBlank()) {
                    Text("Previous: ${s.previousIp}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Rotations this session: ${s.rotations}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Real IP check
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your real IP (without proxy)", style = MaterialTheme.typography.titleSmall)
                Text(myIp ?: "Not checked yet", fontFamily = FontFamily.Monospace)
                OutlinedButton(onClick = { vm.checkMyIp() }, enabled = !checking) {
                    Text(if (checking) "Checking…" else "Check my IP")
                }
            }
        }

        // Setup checklist
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Setup checklist", style = MaterialTheme.typography.titleSmall)
                SetupRow(
                    title = "Floating overlay",
                    desc = "Show timer + IP above other apps",
                    done = overlayGranted,
                    action = "Allow",
                    onAction = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    }
                )
                SetupRow(
                    title = "Notifications",
                    desc = "Status in the notification bar",
                    done = notifGranted,
                    action = "Allow",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                SetupRow(
                    title = "Battery (keep it running)",
                    desc = "Set to “No restrictions”. On Xiaomi / Oppo / Vivo / Realme also enable Autostart.",
                    done = false,
                    action = "Open",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                )
                if (!settings.overlayEnabled) {
                    Text(
                        "Overlay is turned off in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun PowerButton(running: Boolean, onClick: () -> Unit) {
    val colors = if (running) listOf(Color(0xFFE53935), Color(0xFFB71C1C))
    else listOf(Color(0xFF1E4FD8), Color(0xFF0B1F4B))
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (running) Icons.Default.Stop else Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Text(if (running) "STOP" else "START", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SetupRow(title: String, desc: String, done: Boolean, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) Button(onClick = onAction) { Text(action) }
    }
}
