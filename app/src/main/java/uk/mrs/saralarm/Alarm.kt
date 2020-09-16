package uk.mrs.saralarm

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_alarm.*
import java.io.FileInputStream
import java.io.IOException


class Alarm : Activity() {
    private var mp: MediaPlayer? = null
    private var originalAudio = 0
    private var vibrator: Vibrator? = null
    /* access modifiers changed from: protected */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(1)
        window.addFlags(6816896)
        setContentView(R.layout.activity_alarm)

        println(intent.getStringExtra("alarmPreviewSMSBody"))
        alarmPreviewSMSTextView.text = if (intent.getStringExtra("alarmPreviewSMSBody") != null) intent.getStringExtra("alarmPreviewSMSBody") else "Preview not available"
        alarmPreviewSMSNumberTextView.text = if (intent.getStringExtra("alarmPreviewSMSNumber") != null) "From: " + intent.getStringExtra("alarmPreviewSMSNumber") else ""

        mp = MediaPlayer()
        mp!!.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
        mp!!.isLooping = true
        try {
            val fileInputStream = FileInputStream(intent.getStringExtra("soundFile"))
            mp!!.setDataSource(fileInputStream.fd)
            fileInputStream.close()
        } catch (e: IOException) {
            try {
                mp!!.setDataSource(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(applicationContext, 1))
            } catch (e2: IOException) {
                e2.printStackTrace()
            }
        }
        mp!!.prepare()
        mp!!.start()
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        originalAudio = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
        audio.mode =  AudioManager.MODE_IN_CALL
        audio.isSpeakerphoneOn = true

        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = uiOptions


        val pattern = longArrayOf(0, 200, 500)
        // get the vibrator service for the device.
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        vibrator!!.vibrate(pattern, 0)

        val drawable = AnimationDrawable()
        val handler = Handler()
        drawable.addFrame(ColorDrawable(Color.RED), 500)
        drawable.addFrame(ColorDrawable(Color.GREEN), 500)

        drawable.isOneShot = false
        alarm_background.background = drawable

        handler.postDelayed({ drawable.start() }, 100)
        Handler().postDelayed({ finish() }, 90000)

        alarm_stop_button.setOnClickListener { finish() }

        FirebaseAnalytics.getInstance(applicationContext).logEvent("alarm_activity_started", null)
    }

    /* access modifiers changed from: protected */
    public override fun onResume() {
        super.onResume()
        mp?.start()
    }

    /* access modifiers changed from: protected */
    public override fun onPause() {
        super.onPause()
        mp?.pause()
    }

    /* access modifiers changed from: protected */
    public override fun onDestroy() {
        super.onDestroy()
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_VOICE_CALL, originalAudio, 0)
        vibrator!!.cancel()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(0)
        ActivationNotification.notifyPostAlarm(this)
    }

    override fun onBackPressed() {}
}