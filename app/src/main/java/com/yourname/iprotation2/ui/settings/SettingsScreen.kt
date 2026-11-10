@file:OptIn(ExperimentalMaterial3Api::class)

package com.yourname.iprotation2.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.iprotation2.data.model.ProxyType
import com.yourname.iprotation2.data.model.ThemeMode
import com.yourname.iprotation2.ui.components.ScreenScaffold

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var slider by remember(s.intervalSec) { mutableFloatStateOf(s.intervalSec.toFloat()) }

    ScreenScaffold(title = "Settings") { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Rotation interval") {
                Text("${slider.toInt()} seconds", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = slider,
                    onValueChange = { slider = it },
                    onValueChangeFinished = { vm.setInterval(slider.toInt()) },
                    valueRange = 1f..300f
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 5, 10, 30, 60).forEach { v ->
                        FilterChip(
                            selected = s.intervalSec == v,
                            onClick = { vm.setInterval(v) },
                            label = { Text("${v}s") }
                        )
                    }
                }
            }

            SectionCard("Proxy order") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !s.randomOrder, onClick = { vm.setRandomOrder(false) }, label = { Text("Sequential") })
                    FilterChip(selected = s.randomOrder, onClick = { vm.setRandomOrder(true) }, label = { Text("Random") })
                }
                SwitchRow("Auto-skip dead proxies", s.skipDead) { vm.setSkipDead(it) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Retries per rotation", Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { vm.setRetryCount(s.retryCount - 1) }) { Text("−") }
                    Text("${s.retryCount}", fontWeight = FontWeight.Bold)
                    FilledTonalIconButton(onClick = { vm.setRetryCount(s.retryCount + 1) }) { Text("+") }
                }
            }

            SectionCard("Default proxy type") {
                Text(
                    "Used when a pasted line has no scheme (like http:// or socks5://).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProxyType.entries.forEach { t ->
                        FilterChip(selected = s.defaultType == t, onClick = { vm.setDefaultType(t) }, label = { Text(t.label) })
                    }
                }
            }

            SectionCard("Floating overlay") {
                SwitchRow("Show timer + IP over other apps", s.overlayEnabled) { enabled ->
                    vm.setOverlayEnabled(enabled)
                    if (enabled && !Settings.canDrawOverlays(context)) {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    }
                }
                Text(
                    "Drag to move, tap to expand or collapse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("Theme") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { m ->
                        FilterChip(
                            selected = s.theme == m,
                            onClick = { vm.setTheme(m) },
                            label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            SectionCard("Keep it running in background") {
                Text(
                    "Some phones (Xiaomi, Oppo, Vivo, Realme, Samsung) stop background apps. Set battery to “No restrictions” and enable Autostart for this app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }) { Text("Battery settings") }
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        )
                    }) { Text("App info") }
                }
            }

            SectionCard("About") {
                Text("IP Rotation 2.0", fontWeight = FontWeight.Bold)
                Text("Developed by NiROB", color = MaterialTheme.colorScheme.secondary)
                Text(
                    "Proxies are applied to this app's own network checks (the IP shown here). " +
                        "Other apps on the phone are not routed through them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
