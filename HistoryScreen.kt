package com.yourname.iprotation2.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.iprotation2.ui.components.ScreenScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: HistoryViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmClear by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) vm.exportCsv(uri) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    ScreenScaffold(
        title = "IP History",
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = { exportLauncher.launch("ip_history.csv") }) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
            }
            IconButton(onClick = { confirmClear = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear history")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search IP, country or proxy") }
            )
            if (items.isEmpty()) {
                Text(
                    "No history yet.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { h ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(h.ip, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text(
                                        fmt.format(Date(h.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${h.flag} ${h.country}".trim().ifEmpty { "Unknown country" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${h.proxyLabel} · ${h.pingMs} ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All logged IPs will be deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.clear(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
