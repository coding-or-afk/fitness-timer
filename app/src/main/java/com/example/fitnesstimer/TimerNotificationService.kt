package com.example.fitnesstimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * 앱이 백그라운드에 있는 동안 남은 시간을 알림으로 보여준다.
 * 포그라운드 서비스로 떠 있어야 프로세스가 얼지 않아 MainActivity의 타이머와 종료 벨이 계속 동작한다.
 */
class TimerNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val endAt = intent?.getLongExtra(EXTRA_END_AT, 0L) ?: 0L
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(endAt),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        return START_NOT_STICKY
    }

    private fun buildNotification(endAt: Long) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_timer)
        .setContentTitle(getString(R.string.status_running))
        .setContentText(getString(R.string.notification_text))
        // 시스템이 직접 1초마다 줄여서 그려주므로 알림을 갱신할 필요가 없다
        .setWhen(endAt)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setOngoing(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        // Android 16 이상: Live Update로 올려서 상태바 칩에 남은 시간을 바로 보여준다
        .setRequestPromotedOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                packageManager.getLaunchIntentForPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .build()

    companion object {
        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_END_AT = "end_at"

        /** @param remainingMillis 지금부터 타이머가 끝날 때까지 남은 시간 */
        fun start(context: Context, remainingMillis: Long) {
            createChannel(context)
            val intent = Intent(context, TimerNotificationService::class.java)
                .putExtra(EXTRA_END_AT, System.currentTimeMillis() + remainingMillis)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                // 백그라운드 전환 직후의 허용 시간을 놓치면 시작이 거부될 수 있다. 알림만 못 띄울 뿐 타이머는 그대로 둔다.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerNotificationService::class.java))
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
