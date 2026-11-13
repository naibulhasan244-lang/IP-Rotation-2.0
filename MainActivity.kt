package com.yourname.iprotation2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.iprotation2.data.model.ThemeMode
import com.yourname.iprotation2.ui.AppRoot
import com.yourname.iprotation2.ui.settings.SettingsViewModel
import com.yourname.iprotation2.ui.splash.SplashScreen
import com.yourname.iprotation2.ui.theme.IPRotationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsVm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsVm.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            IPRotationTheme(darkTheme = dark) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }
}
