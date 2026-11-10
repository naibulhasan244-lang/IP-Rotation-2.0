package com.yourname.iprotation2.ui.proxies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.iprotation2.data.ProxyRepository
import com.yourname.iprotation2.data.local.ProxyEntity
import com.yourname.iprotation2.data.model.ProxyStatus
import com.yourname.iprotation2.data.model.ProxyType
import com.yourname.iprotation2.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProxyFilter { ALL, WORKING, DEAD }

@HiltViewModel
class ProxiesViewModel @Inject constructor(
    private val repo: ProxyRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ProxyFilter.ALL)
    val filter: StateFlow<ProxyFilter> = _filter.asStateFlow()

    val total: StateFlow<Int> = repo.proxies.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val proxies: StateFlow<List<ProxyEntity>> = combine(repo.proxies, _filter) { list, f ->
        when (f) {
            ProxyFilter.ALL -> list
            ProxyFilter.WORKING -> list.filter { it.status == ProxyStatus.WORKING }
            ProxyFilter.DEAD -> list.filter { it.status == ProxyStatus.DEAD }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultType: StateFlow<ProxyType> = settingsRepo.settings.map { it.defaultType }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProxyType.HTTP)

    private val _testProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val testProgress: StateFlow<Pair<Int, Int>?> = _testProgress.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setFilter(f: ProxyFilter) { _filter.value = f }

    fun add(text: String, type: ProxyType) {
        viewModelScope.launch {
            val r = repo.addBulk(text, type)
            _messages.emit(
                buildString {
                    append("Added ${r.added}")
                    if (r.duplicates > 0) append(", ${r.duplicates} duplicate")
                    if (r.invalid > 0) append(", ${r.invalid} invalid")
                }
            )
        }
    }

    fun testAll() {
        if (_testProgress.value != null || total.value == 0) return
        viewModelScope.launch {
            _testProgress.value = 0 to total.value
            repo.testAll { done, all -> _testProgress.value = done to all }
            _testProgress.value = null
            _messages.emit("Health check finished")
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch { repo.setEnabled(id, enabled) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
    fun deleteDead() = viewModelScope.launch {
        repo.deleteDead()
        _messages.emit("Dead proxies removed")
    }
}
