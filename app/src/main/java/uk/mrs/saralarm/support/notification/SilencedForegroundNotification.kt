/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support.notification

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.CountDownTimer
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.errorprone.annotations.Keep
import uk.mrs.saralarm.R
import java.util.*
import java.util.concurrent.TimeUnit


class SilencedForegroundNotification : Service() {

    private val id = "SilencedAlarm_01"

    var timeLeft = 0L
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {

        fun isServiceAlive(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(50)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var millisseconds = 0L
        if (intent.action.equals("uk.mrs.saralarm.silenceForeground.stop")) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelfResult(startId)
            stopSelf()
            if (::countDownTimer.isInitialized) {
                countDownTimer.cancel()
            }
            return START_NOT_STICKY
        } else if (intent.action.equals("uk.mrs.saralarm.silenceForeground.changeTimer")) {
            if (::countDownTimer.isInitialized) {
                millisseconds =
                    intent.getSerializableExtra("addMillis") as Long + timeLeft
            }
        } else {
            millisseconds = intent.getSerializableExtra("startMills") as Long
        }

        createNotificationChannel()

        startForeground(11, getNotification(millisToHms(millisseconds)))

        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        countDownTimer = object : CountDownTimer(millisseconds, 1000) {
            override fun onTick(millis: Long) {
                timeLeft = millis

                val notification: Notification = getNotification(millisToHms(millis))

                val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(11, notification)
            }

            override fun onFinish() {
                timeLeft = 0
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun getNotification(hms: String): Notification {

        val add1HourBroadcastPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                application, 7000, Intent(application, SilenceBroadcastReceiver::class.java)
                    .putExtra("button", ButtonData.ONE_HOUR), PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
            )
        val add10MinutesBroadcastPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                application, 7001, Intent(application, SilenceBroadcastReceiver::class.java)
                    .putExtra("button", ButtonData.TEN_MINUTES), PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
            )
        val cancelBroadcastPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                application, 7002, Intent(application, SilenceBroadcastReceiver::class.java)
                    .putExtra("button", ButtonData.CANCEL), PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
            )
        if (!::notificationBuilder.isInitialized) {
            val title = "Alarm Silenced"
            notificationBuilder = NotificationCompat.Builder(this, id)
                .setSmallIcon(R.drawable.ic_baseline_notification_important_24)
                .setContentTitle(title)
                .setContentText("Ends in: $hms")
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setColor(Color.argb(255, 204, 51, 1))
                .addAction(R.drawable.ic_baseline_add_24, "+1 Hour", add1HourBroadcastPendingIntent)
                .addAction(R.drawable.ic_baseline_add_24, "+10 Minutes", add10MinutesBroadcastPendingIntent)
                .addAction(R.drawable.ic_baseline_delete_24, "Cancel", cancelBroadcastPendingIntent)
        } else {
            notificationBuilder.setContentText("Ends in: $hms")
        }
        return notificationBuilder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent("uk.mrs.saralarm.RespondFragment.SilencedForegroundNotificationClosed")
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            id,
            "Alarm Silenced",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
    }

    fun millisToHms(millis: Long): String {
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    class SilenceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, i: Intent) {
            when (i.getSerializableExtra("button")) {
                ButtonData.ONE_HOUR -> {
                    val intent = Intent(context, SilencedForegroundNotification::class.java)
                    intent.action = "uk.mrs.saralarm.silenceForeground.changeTimer"
                    intent.putExtra("addMillis", 3600000L) //60 MINUTES
                    context.startService(intent)
                }
                ButtonData.TEN_MINUTES -> {
                    val intent = Intent(context, SilencedForegroundNotification::class.java)
                    intent.action = "uk.mrs.saralarm.silenceForeground.changeTimer"
                    intent.putExtra("addMillis", 600000L) //10 MINUTES
                    context.startService(intent)
                }
                ButtonData.CANCEL -> {
                    Toast.makeText(context, "SARCALL Alarm silence ended!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, SilencedForegroundNotification::class.java)
                    intent.action = "uk.mrs.saralarm.silenceForeground.stop"
                    context.startService(intent)

                }
            }
        }
    }

    @Keep
    enum class ButtonData {
        ONE_HOUR, TEN_MINUTES, CANCEL
    }
}
