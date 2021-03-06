package uk.mrs.saralarm.support.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import uk.mrs.saralarm.MainActivity
import uk.mrs.saralarm.R


object PostAlarmNotification {

    fun create(context: Context) {
        val title = context.resources.getString(R.string.activation_notification_title_template)

        val notificationBuilder = NotificationCompat.Builder(context, "Post Alarm Trigger")
            .setSmallIcon(R.drawable.ic_baseline_notification_important_24)
            .setContentTitle(title)
            .setContentText("SARCALL Alarm activated recently! Tap to respond.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(("SARCALL Alarm activated recently! Tap to respond.")))
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT))
            .setPriority(2)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.argb(255, 204, 51, 1))

        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val chan = NotificationChannel("Post Alarm Trigger", "Post Alarm Trigger", NotificationManager.IMPORTANCE_HIGH)
            chan.setBypassDnd(true)
            notificationManager.createNotificationChannel(chan)

        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}