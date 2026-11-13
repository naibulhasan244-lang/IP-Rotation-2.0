package com.yourname.iprotation2.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourname.iprotation2.data.model.AppSettings
import com.yourname.iprotation2.data.model.ProxyType
import com.yourname.iprotation2.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val INTERVAL = intPreferencesKey("interval_sec")
        val RANDOM = booleanPreferencesKey("random_order")
        val SKIP_DEAD = booleanPreferencesKey("skip_dead")
        val RETRY = intPreferencesKey("retry_count")
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_TYPE = stringPreferencesKey("default_type")
        val OVERLAY = booleanPreferencesKey("overlay_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val d = AppSettings()
            AppSettings(
                intervalSec = p[Keys.INTERVAL] ?: d.intervalSec,
                randomOrder = p[Keys.RANDOM] ?: d.randomOrder,
                skipDead = p[Keys.SKIP_DEAD] ?: d.skipDead,
                retryCount = p[Keys.RETRY] ?: d.retryCount,
                theme = ThemeMode.entries.firstOrNull { it.name == p[Keys.THEME] } ?: d.theme,
                defaultType = ProxyType.fromName(p[Keys.DEFAULT_TYPE] ?: d.defaultType.name),
                overlayEnabled = p[Keys.OVERLAY] ?: d.overlayEnabled
            )
        }

    suspend fun setInterval(v: Int) { context.dataStore.edit { it[Keys.INTERVAL] = v.coerceIn(1, 300) } }
    suspend fun setRandomOrder(v: Boolean) { context.dataStore.edit { it[Keys.RANDOM] = v } }
    suspend fun setSkipDead(v: Boolean) { context.dataStore.edit { it[Keys.SKIP_DEAD] = v } }
    suspend fun setRetryCount(v: Int) { context.dataStore.edit { it[Keys.RETRY] = v.coerceIn(1, 10) } }
    suspend fun setTheme(v: ThemeMode) { context.dataStore.edit { it[Keys.THEME] = v.name } }
    suspend fun setDefaultType(v: ProxyType) { context.dataStore.edit { it[Keys.DEFAULT_TYPE] = v.name } }
    suspend fun setOverlayEnabled(v: Boolean) { context.dataStore.edit { it[Keys.OVERLAY] = v } }
}
