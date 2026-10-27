package com.eventcounter

import android.app.Application
import com.eventcounter.di.AppContainer

class EventCounterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
