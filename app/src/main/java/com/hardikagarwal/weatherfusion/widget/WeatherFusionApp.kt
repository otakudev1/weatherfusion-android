package com.hardikagarwal.weatherfusion.widget

import android.app.Application

/** Application entry: arms the 3h periodic widget refresh. */
class WeatherFusionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetRefresher.schedulePeriodic(this)
    }
}
