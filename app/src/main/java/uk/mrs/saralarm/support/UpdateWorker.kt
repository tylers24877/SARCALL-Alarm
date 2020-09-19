package uk.mrs.saralarm.support

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.firebase.analytics.FirebaseAnalytics

class UpdateWorker(context: Context?, params: WorkerParameters?) : Worker(context!!, params!!) {
    override fun doWork(): Result {
        if (PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("betaChannel", false)) {
            AppUpdater(applicationContext).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.NOTIFICATION)
                .setUpdateXML("http://sarcallapp.com/downloads/sarcall_alarm/update_beta.xml").setCancelable(false).start()
        } else {
            AppUpdater(applicationContext).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.NOTIFICATION).setUpdateXML("http://sarcallapp.com/downloads/sarcall_alarm/update.xml")
                .setCancelable(false).start()
        }
        val param = Bundle()
        param.putString("beta", PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("betaChannel", false).toString())
        FirebaseAnalytics.getInstance(applicationContext).logEvent("background_update_check", param)
        return Result.success()
    }
}