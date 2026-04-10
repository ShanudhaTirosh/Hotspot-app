package com.shanufx.hotspotx.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("hotspotx_prefs")

data class AppSettings(
    val themeMode: String = "system",         // "dark", "light", "system"
    val hotspotSsid: String = "HotspotX",
    val hotspotPassword: String = "hotspotx123",
    val hotspotBand: Int = 0,                 // 0=auto, 1=2.4GHz, 2=5GHz
    val maxClients: Int = 8,
    val hiddenNetwork: Boolean = false,
    val autoOffMinutes: Int = 0,              // 0 = disabled
    val autoStartOnBoot: Boolean = false,
    val foregroundServiceEnabled: Boolean = true,
    val monthlyDataCapBytes: Long = 0L,       // 0 = no cap
    val dataCapWarningPercent: Int = 80,
    val notifyOnDeviceConnect: Boolean = true,
    val notifyOnDataLimit: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val SSID = stringPreferencesKey("ssid")
        val PASSWORD = stringPreferencesKey("password")
        val BAND = intPreferencesKey("band")
        val MAX_CLIENTS = intPreferencesKey("max_clients")
        val HIDDEN = booleanPreferencesKey("hidden")
        val AUTO_OFF = intPreferencesKey("auto_off_minutes")
        val AUTO_BOOT = booleanPreferencesKey("auto_boot")
        val FG_SERVICE = booleanPreferencesKey("fg_service")
        val DATA_CAP = longPreferencesKey("data_cap_bytes")
        val CAP_WARN = intPreferencesKey("cap_warn_percent")
        val NOTIFY_CONNECT = booleanPreferencesKey("notify_connect")
        val NOTIFY_DATA = booleanPreferencesKey("notify_data")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME] ?: "system",
                hotspotSsid = prefs[Keys.SSID] ?: "HotspotX",
                hotspotPassword = prefs[Keys.PASSWORD] ?: "hotspotx123",
                hotspotBand = prefs[Keys.BAND] ?: 0,
                maxClients = prefs[Keys.MAX_CLIENTS] ?: 8,
                hiddenNetwork = prefs[Keys.HIDDEN] ?: false,
                autoOffMinutes = prefs[Keys.AUTO_OFF] ?: 0,
                autoStartOnBoot = prefs[Keys.AUTO_BOOT] ?: false,
                foregroundServiceEnabled = prefs[Keys.FG_SERVICE] ?: true,
                monthlyDataCapBytes = prefs[Keys.DATA_CAP] ?: 0L,
                dataCapWarningPercent = prefs[Keys.CAP_WARN] ?: 80,
                notifyOnDeviceConnect = prefs[Keys.NOTIFY_CONNECT] ?: true,
                notifyOnDataLimit = prefs[Keys.NOTIFY_DATA] ?: true
            )
        }

    suspend fun updateTheme(mode: String) = update { it[Keys.THEME] = mode }
    suspend fun updateSsid(ssid: String) = update { it[Keys.SSID] = ssid }
    suspend fun updatePassword(pw: String) = update { it[Keys.PASSWORD] = pw }
    suspend fun updateBand(band: Int) = update { it[Keys.BAND] = band }
    suspend fun updateMaxClients(max: Int) = update { it[Keys.MAX_CLIENTS] = max }
    suspend fun updateHidden(hidden: Boolean) = update { it[Keys.HIDDEN] = hidden }
    suspend fun updateAutoOff(minutes: Int) = update { it[Keys.AUTO_OFF] = minutes }
    suspend fun updateAutoBoot(enabled: Boolean) = update { it[Keys.AUTO_BOOT] = enabled }
    suspend fun updateForegroundService(enabled: Boolean) = update { it[Keys.FG_SERVICE] = enabled }
    suspend fun updateDataCap(bytes: Long) = update { it[Keys.DATA_CAP] = bytes }
    suspend fun updateCapWarning(percent: Int) = update { it[Keys.CAP_WARN] = percent }
    suspend fun updateNotifyConnect(enabled: Boolean) = update { it[Keys.NOTIFY_CONNECT] = enabled }
    suspend fun updateNotifyData(enabled: Boolean) = update { it[Keys.NOTIFY_DATA] = enabled }

    private suspend fun update(transform: (MutablePreferences) -> Unit) {
        context.dataStore.edit { transform(it) }
    }
}
