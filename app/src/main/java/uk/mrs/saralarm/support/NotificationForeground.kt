package uk.mrs.saralarm.support

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import uk.mrs.saralarm.Alarm
import uk.mrs.saralarm.MainActivity


class NotificationForeground : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val ruleAlarmData = intent.getSerializableExtra("ruleAlarmData") as RuleAlarmData
        val title = resources.getString(uk.mrs.saralarm.R.string.activation_notification_title_template)

        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, "Alarm Trigger Foreground")
            .setSmallIcon(uk.mrs.saralarm.R.drawable.ic_baseline_notification_important_24)
            .setContentTitle(title)
            .setContentText("SARCALL Alarm Activated").setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.argb(255, 204, 51, 1))

        startForeground(10, notificationBuilder.build())

        val fullScreenIntent = Intent(this, Alarm::class.java)
        fullScreenIntent.putExtra("ruleAlarmData", ruleAlarmData)
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK)

        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else true
        ) {
            startActivity(fullScreenIntent)
        } else {
            ActivationNotification.notifyPostAlarm(this)
        }

        stopSelf()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "Alarm Trigger Foreground",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
