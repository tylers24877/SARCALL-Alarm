/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.preference.PreferenceManager
import uk.mrs.saralarm.R


class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetID in appWidgetIds) {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_app)
            remoteViews.setOnClickPendingIntent(
                R.id.appwidget_sarcall_button,
                getPendingSelfIntent(context)
            )
            updateAppWidget(context, appWidgetManager, appWidgetID, remoteViews)
        }
    }

    private fun onUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        onUpdate(
            context,
            appWidgetManager,
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context.packageName,
                    javaClass.name
                )
            )
        )
    }

    override fun onEnabled(context: Context?) {}
    override fun onDisabled(context: Context?) {}
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (WIDGET_CLICK == intent.action) {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)

            when {
                pref.getBoolean("prefEnabled", false) -> {
                    pref.edit().putBoolean("prefEnabled", false).apply()
                    Toast.makeText(context, "SARCALL Alarm disabled", Toast.LENGTH_SHORT).show()
                }
                pref.getString("rulesJSON", "")!!.isNotEmpty() -> {
                    pref.edit().putBoolean("prefEnabled", true).apply()
                    Toast.makeText(context, "SARCALL Alarm enabled", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(
                        context,
                        "Unable to activate SARCALL Alarm, as the rules are not configured in app settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            onUpdate(context)
        }
    }

    private fun getPendingSelfIntent(context: Context): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = WIDGET_CLICK
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val WIDGET_CLICK = "uk.mrs.saralarm.Widget.widgetclick"
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews
        ) {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val widgetText: CharSequence = context.getString(R.string.appwidget_text)
            if (pref.getBoolean("prefEnabled", false)) {
                views.setTextColor(R.id.appwidget_sarcall_button, -16711936)
            } else {
                views.setTextColor(R.id.appwidget_sarcall_button, context.resources.getColor(R.color.design_default_color_error))
            }
            views.setTextViewText(R.id.appwidget_sarcall_button, widgetText)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}