package com.yourname.iprotation2.ui.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.iprotation2.data.local.IpHistoryDao
import com.yourname.iprotation2.data.local.IpHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: IpHistoryDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val items: StateFlow<List<IpHistoryEntity>> = combine(dao.observeAll(), _query) { list, q ->
        val s = q.trim()
        if (s.isEmpty()) list
        else list.filter {
            it.ip.contains(s, ignoreCase = true) ||
                it.country.contains(s, ignoreCase = true) ||
                it.proxyLabel.contains(s, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setQuery(q: String) { _query.value = q }

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val rows = dao.getAll()
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                        w.write("time,ip,country,proxy,ping_ms\n")
                        rows.forEach { r ->
                            w.write(
                                listOf(
                                    fmt.format(Date(r.timestamp)),
                                    r.ip,
                                    r.country,
                                    r.proxyLabel,
                                    r.pingMs.toString()
                                ).joinToString(",") { csv(it) } + "\n"
                            )
                        }
                    } != null
                } catch (e: Exception) {
                    false
                }
            }
            _messages.emit(if (ok) "CSV exported" else "Export failed")
        }
    }

    private fun csv(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
}
