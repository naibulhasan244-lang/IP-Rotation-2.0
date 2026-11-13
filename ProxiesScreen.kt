@file:OptIn(ExperimentalMaterial3Api::class)

package com.yourname.iprotation2.ui.proxies

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.iprotation2.data.local.ProxyEntity
import com.yourname.iprotation2.data.model.ProxyStatus
import com.yourname.iprotation2.data.model.ProxyType
import com.yourname.iprotation2.ui.components.ScreenScaffold

@Composable
fun ProxiesScreen(vm: ProxiesViewModel = hiltViewModel()) {
    val proxies by vm.proxies.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val progress by vm.testProgress.collectAsStateWithLifecycle()
    val defaultType by vm.defaultType.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    ScreenScaffold(
        title = "Proxies ($total)",
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = { vm.testAll() }, enabled = progress == null && total > 0) {
                Icon(Icons.Default.NetworkCheck, contentDescription = "Test all")
            }
            IconButton(onClick = { vm.deleteDead() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Remove dead")
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add proxies")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProxyFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { vm.setFilter(f) },
                        label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            progress?.let { (done, all) ->
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Testing $done / $all", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { if (all == 0) 0f else done.toFloat() / all },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (proxies.isEmpty()) {
                Text(
                    if (total == 0) "No proxies yet.\nTap + and paste your proxy list (one per line)."
                    else "Nothing in this filter.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(proxies, key = { it.id }) { p ->
                        ProxyRow(
                            p = p,
                            onToggle = { vm.setEnabled(p.id, it) },
                            onDelete = { vm.delete(p.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddProxiesDialog(
            initialType = defaultType,
            onDismiss = { showAdd = false },
            onAdd = { text, type ->
                vm.add(text, type)
                showAdd = false
            }
        )
    }
}

@Composable
private fun ProxyRow(p: ProxyEntity, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val statusColor = when (p.status) {
        ProxyStatus.WORKING -> Color(0xFF2E7D32)
        ProxyStatus.DEAD -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.outline
    }
    val statusText = when (p.status) {
        ProxyStatus.WORKING -> "Working · ${p.pingMs} ms"
        ProxyStatus.DEAD -> "Dead"
        else -> "Untested"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("${p.host}:${p.port}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        "${p.type}${if (!p.username.isNullOrEmpty()) " · auth" else ""} · $statusText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = p.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun AddProxiesDialog(
    initialType: ProxyType,
    onDismiss: () -> Unit,
    onAdd: (String, ProxyType) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add proxies") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    placeholder = { Text("1.2.3.4:8080\nsocks5://5.6.7.8:1080\nuser:pass@9.9.9.9:3128") }
                )
                Text(
                    "Type for lines without a scheme (http:// socks5:// …):",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProxyType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                    }
                }
                Text(
                    "Formats: ip:port · ip:port:user:pass · user:pass@ip:port · type://ip:port · type://user:pass@ip:port",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text, type) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
