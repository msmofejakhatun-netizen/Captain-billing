package com.example

import android.app.Application
import com.example.data.di.AppContainer
import com.example.data.di.AppContainerImpl

class CaptainApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("CRASH_LOG", "CRASH DETECTED: UNCAUGHT EXCEPTION", throwable)
            // Optionally, you could also write to a local log file or send to a crash reporter here
        }
    }
}
