package cz.applifting.humansis.ui

import android.app.Application
import cz.applifting.humansis.di.AppComponent
import cz.applifting.humansis.di.DaggerAppComponent
import quanti.com.kotlinlog.Log
import quanti.com.kotlinlog.android.AndroidLogger
import quanti.com.kotlinlog.base.LogLevel
import quanti.com.kotlinlog.base.LoggerBundle
import quanti.com.kotlinlog.file.FileLogger
import quanti.com.kotlinlog.file.bundle.DayLogBundle

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */

class App : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        // Use custom logger
        Log.initialise(this)
        Log.addLogger(AndroidLogger(LoggerBundle(LogLevel.DEBUG)))
        Log.addLogger(FileLogger(applicationContext, DayLogBundle(maxDaysSaved = 3)))
        Log.useUncheckedErrorHandler()

        appComponent = DaggerAppComponent.builder()
            .context(this)
            .build()
    }
}