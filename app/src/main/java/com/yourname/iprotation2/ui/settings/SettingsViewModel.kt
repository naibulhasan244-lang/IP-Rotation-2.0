package com.yourname.iprotation2.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.iprotation2.data.model.AppSettings
import com.yourname.iprotation2.data.model.ProxyType
import com.yourname.iprotation2.data.model.ThemeMode
import com.yourname.iprotation2.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setInterval(v: Int) { viewModelScope.launch { repo.setInterval(v) } }
    fun setRandomOrder(v: Boolean) { viewModelScope.launch { repo.setRandomOrder(v) } }
    fun setSkipDead(v: Boolean) { viewModelScope.launch { repo.setSkipDead(v) } }
    fun setRetryCount(v: Int) { viewModelScope.launch { repo.setRetryCount(v) } }
    fun setTheme(v: ThemeMode) { viewModelScope.launch { repo.setTheme(v) } }
    fun setDefaultType(v: ProxyType) { viewModelScope.launch { repo.setDefaultType(v) } }
    fun setOverlayEnabled(v: Boolean) { viewModelScope.launch { repo.setOverlayEnabled(v) } }
}
