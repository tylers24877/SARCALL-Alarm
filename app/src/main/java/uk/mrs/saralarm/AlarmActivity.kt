package uk.mrs.saralarm

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_alarm.*
import uk.mrs.saralarm.support.RuleAlarmData
import uk.mrs.saralarm.support.notification.PostAlarmNotification
import uk.mrs.saralarm.support.notification.SilencedForegroundNotification
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.SoundType
import java.io.FileInputStream


class AlarmActivity : Activity() {
    private var mp: MediaPlayer? = null
    private var originalAudio = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            this.window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        val ruleAlarmData =
            if (intent.getSerializableExtra("ruleAlarmData") != null) {
                intent.getSerializableExtra("ruleAlarmData") as RuleAlarmData
            } else {
                RuleAlarmData(alarmPreviewSMSBody = "Error Occurred with reading SMS. Error Code = 1")
            }

        alarmPreviewSMSTextView.text = ruleAlarmData.alarmPreviewSMSBody
        alarmPreviewSMSNumberTextView.text = "From: " + ruleAlarmData.alarmPreviewSMSNumber

        mp = MediaPlayer()
        mp!!.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)

        mp!!.isLooping = ruleAlarmData.isLooping

        when (ruleAlarmData.soundType) {
            SoundType.NONE -> {
                try {
                    mp!!.setDataSource(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(applicationContext, 1))
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
            SoundType.SYSTEM -> {
                try {
                    FirebaseCrashlytics.getInstance().log(Uri.parse(ruleAlarmData.soundFile).toString())
                    mp!!.setDataSource(applicationContext, Uri.parse(ruleAlarmData.soundFile))
                } catch (e: Exception) {
                    try {
                        Toast.makeText(applicationContext, "Failed to load system sound. Resorting to default.", Toast.LENGTH_LONG).show()
                        mp!!.setDataSource(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(applicationContext, 1))
                    } catch (e2: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e2)
                    }
                }
            }
            SoundType.CUSTOM -> {
                try {
                    val fileInputStream = FileInputStream(ruleAlarmData.soundFile)
                    FirebaseCrashlytics.getInstance().log(ruleAlarmData.soundFile)

                    mp!!.setDataSource(fileInputStream.fd)
                    fileInputStream.close()
                } catch (e: Exception) {
                    try {
                        mp!!.setDataSource(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(applicationContext, 1))
                    } catch (e2: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e2)
                    }
                }
            }
        }

        try {
            mp!!.prepare()
            mp!!.start()
        } catch (e2: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e2)
        }

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        originalAudio = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
        audio.mode = AudioManager.MODE_IN_CALL
        audio.isSpeakerphoneOn = true


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            window.decorView.systemUiVisibility = uiOptions
        } else {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        val drawable = AnimationDrawable()

        val colourArray = ruleAlarmData.colorArrayList
        if (!colourArray.isNullOrEmpty()) {
            for (colour in colourArray) {
                try {
                    drawable.addFrame(ColorDrawable(Color.parseColor(colour)), 500)
                } catch (e: Exception) {
                    when (e) {
                        is IllegalArgumentException, is StringIndexOutOfBoundsException -> {
                            FirebaseCrashlytics.getInstance().recordException(e)
                        }
                        else -> throw e
                    }
                }
            }
            if (drawable.numberOfFrames == 0) {
                drawable.addFrame(ColorDrawable(Color.RED), 500)
                drawable.addFrame(ColorDrawable(Color.GREEN), 500)
            }
        } else {
            drawable.addFrame(ColorDrawable(Color.RED), 500)
            drawable.addFrame(ColorDrawable(Color.GREEN), 500)
        }
        drawable.isOneShot = false
        alarm_background.background = drawable

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({ drawable.start() }, 100)
        handler.postDelayed({ finish() }, 300000)

        alarm_stop_button.setOnClickListener { finish() }

        alarm_silence_button.setOnClickListener {
            if (!SilencedForegroundNotification.isServiceAlive(applicationContext, SilencedForegroundNotification::class.java)) {
                val serviceIntent = Intent(this, SilencedForegroundNotification::class.java)
                serviceIntent.putExtra("startMills", 3600000L)
                ContextCompat.startForegroundService(this, serviceIntent)
                Toast.makeText(this, "Alarm Silenced.", Toast.LENGTH_SHORT).show()
                FirebaseAnalytics.getInstance(applicationContext).logEvent("alarm_silence_started", null)
                finish()
            }
        }

        FirebaseAnalytics.getInstance(applicationContext).logEvent("alarm_activity_started", null)
    }

    override fun onResume() {
        super.onResume()
        mp?.start()
    }

    override fun onPause() {
        super.onPause()
        mp?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mp?.stop()
        mp = null
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_VOICE_CALL, originalAudio, 0)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(0)
        PostAlarmNotification.create(this)
    }

    override fun onBackPressed() {}
}