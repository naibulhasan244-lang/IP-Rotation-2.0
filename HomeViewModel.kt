package com.yourname.iprotation2.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.iprotation2.data.ProxyRepository
import com.yourname.iprotation2.data.model.AppSettings
import com.yourname.iprotation2.data.network.IpChecker
import com.yourname.iprotation2.data.settings.SettingsRepository
import com.yourname.iprotation2.engine.RotationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val engine: RotationEngine,
    private val checker: IpChecker,
    settingsRepo: SettingsRepository,
    proxyRepo: ProxyRepository
) : ViewModel() {

    val state = engine.state

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val proxyCount: StateFlow<Int> = proxyRepo.proxies.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _myIp = MutableStateFlow<String?>(null)
    val myIp: StateFlow<String?> = _myIp.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    fun stop() = engine.stop()

    fun checkMyIp() {
        if (_checking.value) return
        viewModelScope.launch {
            _checking.value = true
            val result = checker.check(null)
            _myIp.value = result.fold(
                onSuccess = { "${it.flag} ${it.ip}  ${it.country}".trim() },
                onFailure = { "Failed: ${it.message ?: "no internet?"}" }
            )
            _checking.value = false
        }
    }
}
