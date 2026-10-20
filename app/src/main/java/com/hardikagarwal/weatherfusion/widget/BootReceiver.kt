package com.hardikagarwal.weatherfusion.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Refresh widgets after device boot (WorkManager jobs are persisted, this just accelerates first paint). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WidgetRefresher.schedulePeriodic(context)
            WidgetRefresher.requestRefresh(context)
        }
    }
}
