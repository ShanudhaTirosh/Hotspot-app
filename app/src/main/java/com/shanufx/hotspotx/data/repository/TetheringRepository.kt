package com.shanufx.hotspotx.data.repository

import android.content.Context
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "TetheringRepo"

sealed class HotspotState {
    object Idle     : HotspotState()
    object Starting : HotspotState()
    data class Active(val ssid: String, val password: String, val band: Int) : HotspotState()
    data class Error(val message: String) : HotspotState()
}

data class HotspotConfig(
    val ssid: String       = "HotspotX",
    val password: String   = "hotspotx123",
    val band: Int          = SoftApConfiguration.BAND_2GHZ,
    val maxClients: Int    = 8,
    val isHidden: Boolean  = false
)

@Singleton
class TetheringRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager
) {
    private val _hotspotState = MutableStateFlow<HotspotState>(HotspotState.Idle)
    val hotspotState: StateFlow<HotspotState> = _hotspotState.asStateFlow()

    private var loHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    suspend fun startHotspot(config: HotspotConfig): Result<Unit> {
        _hotspotState.value = HotspotState.Starting
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val result = tryTetheringManager(config)
            if (result.isFailure) startLocalOnlyHotspot() else result
        } else {
            startLocalOnlyHotspot()
        }
    }

    fun stopHotspot() {
        tryStopTetheringManager()
        loHotspotReservation?.close()
        loHotspotReservation = null
        _hotspotState.value = HotspotState.Idle
    }

    fun isHotspotActive(): Boolean = _hotspotState.value is HotspotState.Active

    // ── LocalOnlyHotspot (API 26+) ──────────────────────────────
    @Suppress("DEPRECATION")
    private suspend fun startLocalOnlyHotspot(): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            try {
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        loHotspotReservation = reservation
                        val (ssid, pass) = extractSsidPass(reservation)
                        _hotspotState.value = HotspotState.Active(ssid, pass, SoftApConfiguration.BAND_2GHZ)
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }
                    override fun onStopped() {
                        loHotspotReservation = null
                        _hotspotState.value = HotspotState.Idle
                    }
                    override fun onFailed(reason: Int) {
                        val msg = "LocalOnlyHotspot failed (reason=$reason). Ensure Location permission is granted."
                        Log.e(TAG, msg)
                        _hotspotState.value = HotspotState.Error(msg)
                        if (cont.isActive) cont.resume(Result.failure(Exception(msg)))
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error starting hotspot"
                _hotspotState.value = HotspotState.Error(msg)
                if (cont.isActive) cont.resume(Result.failure(e))
            }
            cont.invokeOnCancellation { loHotspotReservation?.close() }
        }

    @Suppress("DEPRECATION")
    private fun extractSsidPass(reservation: WifiManager.LocalOnlyHotspotReservation): Pair<String, String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cfg = reservation.softApConfiguration
            (cfg?.ssid ?: "HotspotX") to (cfg?.passphrase ?: "")
        } else {
            val wc = reservation.wifiConfiguration
            (wc?.SSID ?: "HotspotX") to (wc?.preSharedKey ?: "")
        }
    }

    // ── TetheringManager reflection (API 30+, system/privileged only) ──
    private fun tryTetheringManager(config: HotspotConfig): Result<Unit> {
        return try {
            val tmClass = Class.forName("android.net.TetheringManager")
            val tm = context.getSystemService(tmClass)
                ?: return Result.failure(Exception("TetheringManager unavailable"))

            val reqBuilderClass = Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
            val builder = reqBuilderClass.getConstructor(Int::class.java).newInstance(0) // TETHERING_WIFI=0
            val request = reqBuilderClass.getMethod("build").invoke(builder)
            val reqClass = Class.forName("android.net.TetheringManager\$TetheringRequest")
            val callbackClass = Class.forName("android.net.TetheringManager\$StartTetheringCallback")

            tmClass.getMethod("startTethering", reqClass, java.util.concurrent.Executor::class.java, callbackClass)
                .invoke(tm, request, context.mainExecutor, null)

            _hotspotState.value = HotspotState.Active(config.ssid, config.password, config.band)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "TetheringManager not accessible (non-privileged app): ${e.message}")
            Result.failure(e)
        }
    }

    private fun tryStopTetheringManager() {
        try {
            val tmClass = Class.forName("android.net.TetheringManager")
            val tm = context.getSystemService(tmClass) ?: return
            tmClass.getMethod("stopTethering", Int::class.java).invoke(tm, 0)
        } catch (_: Exception) {}
    }
}
