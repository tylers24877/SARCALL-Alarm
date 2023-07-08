/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import uk.mrs.saralarm.AlarmActivity
import uk.mrs.saralarm.MainActivity
import uk.mrs.saralarm.support.RuleAlarmData


class AlarmForegroundNotification : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val ruleAlarmData = intent.getSerializableExtra("ruleAlarmData") as RuleAlarmData
        val title = resources.getString(uk.mrs.saralarm.R.string.activation_notification_title_template)


        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, "Alarm Trigger Foreground")
            .setSmallIcon(uk.mrs.saralarm.R.drawable.ic_baseline_notification_important_24)
            .setContentTitle(title)
            .setContentText("SARCALL Alarm Activated").setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.argb(255, 204, 51, 1))

        startForeground(10, notificationBuilder.build())

        val fullScreenIntent = Intent(this, AlarmActivity::class.java)
        fullScreenIntent.putExtra("ruleAlarmData", ruleAlarmData)
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK)

        if (
            Settings.canDrawOverlays(this)
        ) {
            startActivity(fullScreenIntent)
        } else {
            PostAlarmNotification.create(this)
        }

        stopSelf()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channelId = "AlarmTriggerForeground"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channelName = "Foreground Service Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val serviceChannel = NotificationChannel(channelId, channelName, importance)

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
