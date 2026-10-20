package com.hardikagarwal.weatherfusion.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.hardikagarwal.weatherfusion.R
import com.hardikagarwal.weatherfusion.domain.WeatherSnapshot

/** 4x2 widget: everything 4x1 shows plus a 3-day strip. */
class WeatherFusionAppWidget4x2 : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetRefresher.requestRefresh(context)
    }

    companion object {
        fun push(context: Context, snapshot: WeatherSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherFusionAppWidget4x2::class.java))
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.widget_4x2).apply {
                setTextViewText(R.id.widget_place, WidgetRender.placeLine(snapshot))
                setTextViewText(R.id.widget_temp, WidgetRender.tempLine(snapshot))
                setTextViewText(R.id.widget_condition, WidgetRender.conditionLine(snapshot))
                setTextViewText(R.id.widget_confidence, WidgetRender.confidenceLine(snapshot))
                setTextColor(R.id.widget_confidence, WidgetRender.confidenceBadgeColor(snapshot))
                setTextViewText(R.id.widget_days, WidgetRender.dayLines(snapshot, 3).joinToString("\n"))
                val alert = WidgetRender.alertLine(snapshot.alerts)
                if (alert == null) {
                    setViewVisibility(R.id.widget_alert_strip, android.view.View.GONE)
                } else {
                    setViewVisibility(R.id.widget_alert_strip, android.view.View.VISIBLE)
                    setTextViewText(R.id.widget_alert_strip, alert)
                    setInt(R.id.widget_alert_strip, "setBackgroundColor", WidgetRender.alertColor(snapshot.alerts) ?: 0)
                }
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
