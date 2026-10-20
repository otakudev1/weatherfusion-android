package com.hardikagarwal.weatherfusion.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Central scheduling for widget data refreshes.
 * - Periodic 3h WorkManager job keeps all providers fresh.
 * - One-shot expedited-ish job after dashboard loads / place changes / boot.
 */
object WidgetRefresher {

    private const val PERIODIC_WORK_NAME = "weatherfusion_widget_periodic"
    private const val ONESHOT_WORK_NAME = "weatherfusion_widget_oneshot"

    /** Call once from Application.onCreate: 3h periodic refresh for the widgets. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(3, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Immediate refresh (dashboard load, boot, config change). */
    fun requestRefresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
