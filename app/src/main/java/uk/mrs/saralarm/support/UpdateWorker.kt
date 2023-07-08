/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import uk.mrs.saralarm.BuildConfig
import uk.mrs.saralarm.MainActivity
import uk.mrs.saralarm.R
import java.text.DateFormat.getDateTimeInstance
import java.util.*

class UpdateWorker(context: Context?, params: WorkerParameters?) : Worker(context!!, params!!) {
    override fun doWork(): Result {
        val remoteConfig = Firebase.remoteConfig

        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        val gson = Gson()
        val remote = remoteConfig.fetchAndActivate()
        remote.addOnSuccessListener {
            val stringJson = remoteConfig.getString("update")
            if (stringJson.isNotEmpty()) {
                val jsonModel = gson.fromJson(stringJson, UpdateUtil.VersionData::class.java)
                val vD = UpdateUtil.VersionData(
                    version = jsonModel.version,
                    version_code = jsonModel.version_code,
                    url = jsonModel.url,
                    release_notes = jsonModel.release_notes
                )

                if(vD.version_code > BuildConfig.VERSION_CODE) {
                    showNotification(applicationContext,vD)
                }
            }
        }

        val param = Bundle()
        param.putString("beta", PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("betaChannel", false).toString())
        FirebaseAnalytics.getInstance(applicationContext).logEvent("background_update_check", param)

        val sdf = getDateTimeInstance()
        val currentDate = sdf.format(Date())
        val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
        editor.putInt("WorkerCount", PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt("WorkerCount", 0) + 1)
        editor.putString("WorkerTime", currentDate).apply()

        return Result.success()
    }

    private fun showNotification(context: Context, versionData: UpdateUtil.VersionData) {
        val title = "Sarcall Alarm Update"

        val notificationBuilder = NotificationCompat.Builder(context, "App Update")
            .setSmallIcon(R.drawable.ic_baseline_notification_important_24)
            .setContentTitle(title)
            .setContentText("Update "+versionData.version+" available for download. Please click to view")
            .setStyle(NotificationCompat.BigTextStyle().bigText(("Update "+versionData.version+" available for download. Please click to view")))
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT))
            .setPriority(2)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(Color.argb(255, 204, 51, 1))

        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel("App Update", "App Update", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(1999, notificationBuilder.build())
    }
}