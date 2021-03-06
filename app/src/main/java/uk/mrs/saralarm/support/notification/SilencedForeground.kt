package uk.mrs.saralarm.support.notification

import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import uk.mrs.saralarm.R
import java.util.*
import java.util.concurrent.TimeUnit


class SilencedForeground : Service() {

    private val id = "SilencedAlarm_01"
    var timeLeft = 0L

    lateinit var countDownTimer: CountDownTimer

    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        var isRunning = false
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var millisseconds = 0L
        if (intent.action.equals("uk.mrs.saralarm.silenceForeground.stop")) {
            stopForeground(true)
            stopSelfResult(startId)
            stopSelf()
            if (::countDownTimer.isInitialized) {
                countDownTimer.cancel()
            }
            return START_NOT_STICKY
        } else if (intent.action.equals("uk.mrs.saralarm.silenceForeground.changeTimer")) {
            if (::countDownTimer.isInitialized) {
                millisseconds = intent.getSerializableExtra("addMillis") as Long + timeLeft
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

        isRunning = true

        return START_NOT_STICKY
    }

    private fun getNotification(hms: String): Notification {

        val add1HourBroadcastPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                application, 7000, Intent(application, SilenceBroadcastReceiver::class.java)
                    .putExtra("button", "add1Hour"), FLAG_UPDATE_CURRENT
            )
        val add10MinutesBroadcastPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                application, 7001, Intent(application, SilenceBroadcastReceiver::class.java)
                    .putExtra("button", "add10Minutes"), FLAG_UPDATE_CURRENT
            )
        val cancelBroadcastPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                application, 7002, Intent(application, SilenceBroadcastReceiver::class.java)
                    .putExtra("button", "cancel"), FLAG_UPDATE_CURRENT
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
                .addAction(R.drawable.ic_baseline_add_24, "Extend 1 Hour", add1HourBroadcastPendingIntent)
                .addAction(R.drawable.ic_baseline_add_24, "Extend 10 Minutes", add10MinutesBroadcastPendingIntent)
                .addAction(R.drawable.ic_baseline_delete_24, "Cancel", cancelBroadcastPendingIntent)
        } else {
            notificationBuilder.setContentText("Ends in: $hms")
        }
        return notificationBuilder.build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                id,
                "Alarm Silenced",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    fun millisToHms(millis: Long): String {
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    class SilenceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, i: Intent) {
            when (i.getSerializableExtra("button")) {
                "add1Hour" -> {
                    val intent = Intent(context, SilencedForeground::class.java)
                    intent.action = "uk.mrs.saralarm.silenceForeground.changeTimer"
                    intent.putExtra("addMillis", 3600000L) //60 MINUTES
                    context.startService(intent)
                }
                "add10Minutes" -> {
                    val intent = Intent(context, SilencedForeground::class.java)
                    intent.action = "uk.mrs.saralarm.silenceForeground.changeTimer"
                    intent.putExtra("addMillis", 600000L) //10 MINUTES
                    context.startService(intent)
                }
                "cancel" -> {
                    Toast.makeText(context, "SARCALL Alarm silence ended!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, SilencedForeground::class.java)
                    intent.action = "uk.mrs.saralarm.silenceForeground.stop"
                    context.startService(intent)
                }
            }
        }
    }
}
