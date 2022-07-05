/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import java.text.DateFormat.getDateTimeInstance
import java.util.*

class UpdateWorker(context: Context?, params: WorkerParameters?) : Worker(context!!, params!!) {
    override fun doWork(): Result {
        if (PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("betaChannel", false)) {
            AppUpdater(applicationContext).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.NOTIFICATION)
                .setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update_beta.xml").setCancelable(false).start()
        } else {
            AppUpdater(applicationContext).setUpdateFrom(UpdateFrom.XML)
                .setDisplay(Display.NOTIFICATION).setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update.xml")
                .setCancelable(false).start()
        }
        val param = Bundle()
        param.putString("beta", PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("betaChannel", false).toString())
        //FirebaseAnalytics.getInstance(applicationContext).logEvent("background_update_check", param)

        val sdf = getDateTimeInstance()
        val currentDate = sdf.format(Date())
        val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
        editor.putInt("WorkerCount", PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt("WorkerCount", 0) + 1)
        editor.putString("WorkerTime", currentDate).apply()

        return Result.success()
    }
}