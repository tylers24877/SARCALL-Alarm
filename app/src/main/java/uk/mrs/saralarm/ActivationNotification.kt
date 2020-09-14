package uk.mrs.saralarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat


object ActivationNotification {

    fun notify(context: Context, soundFile: String, alarmPreviewSMSBody: String, alarmPreviewSMSNumber: String) {
        val title = context.resources.getString(R.string.activation_notification_title_template)

        val fullScreenIntent = Intent(context, Alarm::class.java)
        fullScreenIntent.putExtra("soundFile", soundFile).putExtra("alarmPreviewSMSBody", alarmPreviewSMSBody).putExtra("alarmPreviewSMSNumber", alarmPreviewSMSNumber)
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY).addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        val fullScreenPendingIntent = PendingIntent.getActivity(context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = context.getString(R.string.notification_alarm_channel_id)
        val notificationBuilder = NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_baseline_notification_important_24).setContentTitle(title).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM).setColor(Color.argb(255, 204, 51, 1))
            .setFullScreenIntent(fullScreenPendingIntent, true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH))
        }
        notificationManager.notify(0, notificationBuilder.build())
        return
    }

    fun notifyPostAlarm(context: Context) {
        val title = context.resources.getString(R.string.activation_notification_title_template)

        val notificationBuilder = NotificationCompat.Builder(context, "Post Alarm Trigger").setSmallIcon(R.drawable.ic_baseline_notification_important_24).setContentTitle(title)
            .setContentText("SARCALL activity_alarm triggered!").setStyle(NotificationCompat.BigTextStyle().bigText("SARCALL alarm triggered! Click to respond.")).setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT)).setPriority(2)
            .setCategory(NotificationCompat.CATEGORY_ALARM).setColor(Color.argb(255, 204, 51, 1))

        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel("Post Alarm Trigger", "Post Alarm Trigger", NotificationManager.IMPORTANCE_HIGH))
        }
        notificationManager.notify(1, notificationBuilder.build())
        return
    }
}