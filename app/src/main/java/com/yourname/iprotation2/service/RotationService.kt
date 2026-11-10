package com.yourname.iprotation2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.yourname.iprotation2.MainActivity
import com.yourname.iprotation2.R
import com.yourname.iprotation2.engine.RotationEngine
import com.yourname.iprotation2.engine.RotationState
import com.yourname.iprotation2.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Helper used by the UI to start/stop the rotation service. */
object RotationController {
    fun start(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, RotationService::class.java))
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, RotationService::class.java))
    }
}

@AndroidEntryPoint
class RotationService : Service() {

    @Inject lateinit var engine: RotationEngine
    @Inject lateinit var settingsRepo: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: OverlayController? = null
    private var begun = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            engine.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!begun) {
            begun = true
            begin()
        }
        return START_NOT_STICKY
    }

    private fun begin() {
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(RotationState(status = "Starting…")), type)

        engine.start()
        val controller = OverlayController(this) { engine.stop() }
        overlay = controller

        // Show / hide the overlay depending on the setting and the permission.
        scope.launch {
            settingsRepo.settings.map { it.overlayEnabled }.distinctUntilChanged().collect { enabled ->
                if (enabled && Settings.canDrawOverlays(this@RotationService)) controller.show()
                else controller.hide()
            }
        }

        // Mirror engine state to the notification + overlay; stop service when engine stops.
        scope.launch {
            engine.state.collect { s ->
                if (!s.running) {
                    stopSelf()
                    return@collect
                }
                postNotification(buildNotification(s))
                controller.update(s)
            }
        }
    }

    override fun onDestroy() {
        engine.stop()
        overlay?.hide()
        overlay = null
        scope.cancel()
        super.onDestroy()
    }

    private fun postNotification(n: Notification) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, n)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "IP rotation status", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Shows the current IP and the countdown to the next change"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(s: RotationState): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, RotationService::class.java).setAction(ACTION_STOP),
            flags
        )
        val text = if (s.ip.isBlank()) s.status else "${s.ip} • next change in ${s.countdown}s"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rotate)
            .setContentTitle("IP Rotation 2.0 is running")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.yourname.iprotation2.action.STOP"
        private const val CHANNEL_ID = "rotation_status"
        private const val NOTIF_ID = 1001
    }
}
