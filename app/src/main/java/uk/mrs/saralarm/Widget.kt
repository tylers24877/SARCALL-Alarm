package uk.mrs.saralarm

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.internal.view.SupportMenu
import androidx.preference.PreferenceManager


class Widget : AppWidgetProvider() {
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
                getPendingSelfIntent(context, WIDGET_CLICK)
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
                        "Unable to activate SARCALL Alarm. No activation method chosen in app settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            onUpdate(context)
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, 0)
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
                views.setTextColor(R.id.appwidget_sarcall_button, SupportMenu.CATEGORY_MASK)
            }
            views.setTextViewText(R.id.appwidget_sarcall_button, widgetText)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}