package com.yourname.iprotation2.engine

import com.yourname.iprotation2.data.local.IpHistoryDao
import com.yourname.iprotation2.data.local.IpHistoryEntity
import com.yourname.iprotation2.data.local.ProxyDao
import com.yourname.iprotation2.data.local.ProxyEntity
import com.yourname.iprotation2.data.local.label
import com.yourname.iprotation2.data.model.AppSettings
import com.yourname.iprotation2.data.model.ProxyStatus
import com.yourname.iprotation2.data.network.IpChecker
import com.yourname.iprotation2.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RotationState(
    val running: Boolean = false,
    val countdown: Int = 0,
    val total: Int = 0,
    val ip: String = "",
    val previousIp: String = "",
    val country: String = "",
    val flag: String = "",
    val pingMs: Long = -1,
    val proxyLabel: String = "",
    val status: String = "Idle",
    val error: String? = null,
    val rotations: Int = 0
)

/**
 * Core loop: every N seconds pick the next proxy, verify it by asking an IP service
 * through it, record the IP in history, and publish state for UI / notification / overlay.
 */
@Singleton
class RotationEngine @Inject constructor(
    private val proxyDao: ProxyDao,
    private val historyDao: IpHistoryDao,
    private val checker: IpChecker,
    private val settingsRepo: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(RotationState())
    val state: StateFlow<RotationState> = _state.asStateFlow()

    private var job: Job? = null
    private var lastProxyId = 0L

    fun start() {
        if (job?.isActive == true) return
        lastProxyId = 0L
        _state.value = RotationState(running = true, status = "Starting…")
        job = scope.launch { runLoop() }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(running = false, countdown = 0, status = "Stopped") }
    }

    private suspend fun runLoop() {
        while (scope.isActive) {
            val settings = settingsRepo.settings.first()
            val ok = rotateOnce(settings)
            val wait = if (ok) settings.intervalSec else minOf(settings.intervalSec, 5)
            for (t in wait downTo 1) {
                _state.update { it.copy(countdown = t, total = wait) }
                delay(1000)
            }
        }
    }

    private suspend fun rotateOnce(s: AppSettings): Boolean {
        val all = proxyDao.getEnabled()
        if (all.isEmpty()) {
            fail("No enabled proxies. Add proxies in the Proxies tab.")
            return false
        }
        var candidates = if (s.skipDead) all.filter { it.status != ProxyStatus.DEAD } else all
        if (candidates.isEmpty()) candidates = all // everything marked dead: give them another chance

        val tried = mutableSetOf<Long>()
        val maxAttempts = maxOf(1, s.retryCount)
        var attempts = 0
        while (attempts < maxAttempts && tried.size < candidates.size) {
            val proxy = pick(candidates, tried, s.randomOrder)
            tried += proxy.id
            attempts++
            _state.update { it.copy(status = "Connecting via ${proxy.label()}…", error = null) }

            val result = checker.check(proxy)
            val now = System.currentTimeMillis()
            val info = result.getOrNull()
            if (info != null) {
                proxyDao.updateStatus(proxy.id, ProxyStatus.WORKING, info.pingMs, now)
                historyDao.insert(
                    IpHistoryEntity(
                        ip = info.ip,
                        country = info.country,
                        flag = info.flag,
                        proxyLabel = proxy.label(),
                        pingMs = info.pingMs,
                        timestamp = now
                    )
                )
                historyDao.trim()
                _state.update {
                    it.copy(
                        previousIp = it.ip,
                        ip = info.ip,
                        country = info.country,
                        flag = info.flag,
                        pingMs = info.pingMs,
                        proxyLabel = proxy.label(),
                        status = "Connected",
                        error = null,
                        rotations = it.rotations + 1
                    )
                }
                return true
            } else {
                proxyDao.updateStatus(proxy.id, ProxyStatus.DEAD, -1, now)
            }
        }
        fail("All $attempts attempt(s) failed. Retrying soon…")
        return false
    }

    private fun fail(message: String) {
        _state.update { it.copy(status = "Error", error = message, pingMs = -1) }
    }

    private fun pick(candidates: List<ProxyEntity>, tried: Set<Long>, random: Boolean): ProxyEntity {
        val remaining = candidates.filter { it.id !in tried }
        if (random) return remaining.random()
        val next = remaining.firstOrNull { it.id > lastProxyId } ?: remaining.first()
        lastProxyId = next.id
        return next
    }
}
