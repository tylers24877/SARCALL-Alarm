/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.STREAM_VOICE_CALL
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import uk.mrs.saralarm.databinding.ActivityAlarmBinding
import uk.mrs.saralarm.support.RuleAlarmData
import uk.mrs.saralarm.support.notification.PostAlarmNotification
import uk.mrs.saralarm.support.notification.SilencedForegroundNotification
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.SoundType
import java.io.FileInputStream


class AlarmActivity : AppCompatActivity() {
    private var mp: MediaPlayer? = null
    private var originalAudio = 0

    private lateinit var binding: ActivityAlarmBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                RuleAlarmData(alarmPreviewSMSBody = getString(R.string.alarm_activity_error_Code_1))
            }

        binding.apply {
            alarmPreviewSmsTextView.text = ruleAlarmData.alarmPreviewSMSBody
            alarmPreviewSmsNumberTextView.text = getString(R.string.alarm_activity_preview_sms_number, ruleAlarmData.alarmPreviewSMSNumber)
        }

        mp = MediaPlayer()
        mp!!.setAudioAttributes(
            AudioAttributes.Builder()
                .setLegacyStreamType(STREAM_VOICE_CALL)
                .build()
        )
        mp!!.isLooping = ruleAlarmData.isLooping

        when (ruleAlarmData.soundType) {
            SoundType.NONE -> {
                try {
                    mp!!.setDataSource(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(applicationContext, 1))
                } catch (e: Exception) {
                }
            }
            SoundType.SYSTEM -> {
                try {
                    mp!!.setDataSource(applicationContext, Uri.parse(ruleAlarmData.soundFile))
                } catch (e: Exception) {
                    try {
                        Toast.makeText(applicationContext, getString(R.string.alarm_activity_sound_load_failed), Toast.LENGTH_LONG).show()
                        mp!!.setDataSource(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(applicationContext, 1))
                    } catch (e2: Exception) {
                    }
                }
            }
            SoundType.CUSTOM -> {
                try {
                    val fileInputStream = FileInputStream(ruleAlarmData.soundFile)

                    mp!!.setDataSource(fileInputStream.fd)
                    fileInputStream.close()
                } catch (e: Exception) {
                    try {
                        mp!!.setDataSource(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(applicationContext, 1))
                    } catch (e2: Exception) {
                    }
                }
            }
        }

        try {
            mp!!.prepare()
            mp!!.start()
        } catch (e2: Exception) {
        }

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val maxVolume = audio.getStreamMaxVolume(STREAM_VOICE_CALL)
        originalAudio = audio.getStreamVolume(STREAM_VOICE_CALL)
        audio.setStreamVolume(STREAM_VOICE_CALL, maxVolume, 0)
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
        if (colourArray.isNotEmpty()) {
            for (colour in colourArray) {
                try {
                    drawable.addFrame(ColorDrawable(Color.parseColor(colour)), 500)
                } catch (e: Exception) {
                    when (e) {
                        is IllegalArgumentException, is StringIndexOutOfBoundsException -> {
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
        binding.alarmBackground.background = drawable

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({ drawable.start() }, 100)
        handler.postDelayed({ finish() }, 300000)

        binding.alarmStopButton.setOnClickListener { finish() }

        binding.alarmSilenceButton.setOnClickListener {
            if (!SilencedForegroundNotification.isServiceAlive(applicationContext, SilencedForegroundNotification::class.java)) {
                //start the foreground service for managing the silence function.
                val serviceIntent = Intent(this, SilencedForegroundNotification::class.java)
                serviceIntent.putExtra("startMills", 3600000L)
                ContextCompat.startForegroundService(this, serviceIntent)

                Toast.makeText(this, "Alarm Silenced.", Toast.LENGTH_SHORT).show()
                //stop the alarm
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //resume the alarm sound if the app was paused.
        mp?.start()
    }

    override fun onPause() {
        super.onPause()
        //pause the alarm then the app is paused.
        mp?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        //when app is destroy/finish() called, stop the alarm sound and set the volume levels back to original before the alarm was started.
        mp?.stop()
        mp = null
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(STREAM_VOICE_CALL, originalAudio, 0)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(0)
        PostAlarmNotification.create(this)
    }

    override fun onBackPressed() {} //override the back button to do nothing.
}