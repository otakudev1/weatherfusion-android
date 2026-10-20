package com.hardikagarwal.weatherfusion.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hardikagarwal.weatherfusion.data.LocationStore
import com.hardikagarwal.weatherfusion.data.WeatherRepository

/**
 * Fetches the snapshot for the last-searched location and renders it into
 * every WeatherFusion widget. Failures leave the last rendered RemoteViews
 * untouched (stale data beats a blank widget).
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val place = LocationStore(appContext).load() ?: return Result.success()
        return try {
            val snapshot = WeatherRepository().snapshot(place)
            WeatherFusionAppWidget4x1.push(appContext, snapshot)
            WeatherFusionAppWidget4x2.push(appContext, snapshot)
            Result.success()
        } catch (e: Exception) {
            Result.retry() // network blips: try again within WorkManager backoff
        }
    }
}
