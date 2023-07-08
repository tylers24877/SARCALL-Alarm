/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import android.app.AppOpsManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.provider.Settings
import android.text.Html
import android.text.Spanned
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics

object Util {
    fun fromHtml(string: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(string)
        }
    }
    fun canDrawOverlays(context: Context): Boolean {
        if (Settings.canDrawOverlays(context)) return true
        //USING APP OPS MANAGER
        val manager = context.getSystemService(AppCompatActivity.APP_OPS_SERVICE) as AppOpsManager
        try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                manager.unsafeCheckOp(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Binder.getCallingUid(), context.packageName)
            } else {
                manager.checkOp(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Binder.getCallingUid(), context.packageName)
            }
            return result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        try { //IF This Fails, we definitely can't do it
            val mgr = context.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
            //getSystemService might return null
            val viewToAdd = View(context)
            val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams(
                    0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSPARENT
                )
            } else {
                WindowManager.LayoutParams(
                    0, // Width (0 means determined by the system)
                    0, // Height (0 means determined by the system)
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // Type: System-level alert window
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Flags
                    PixelFormat.TRANSPARENT // Pixel format: Transparent
                )
            }
            viewToAdd.layoutParams = params
            mgr.addView(viewToAdd, params)
            mgr.removeView(viewToAdd)
            return true
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)

        }
        return false
    }
}