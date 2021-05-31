package cz.applifting.humansis.ui

import android.app.Application
import cz.applifting.humansis.di.AppComponent
import cz.applifting.humansis.di.DaggerAppComponent
import cz.quanti.android.nfc.logger.NfcLogger
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
        NfcLogger.registerListener(Logger())

        appComponent = DaggerAppComponent.builder()
            .app(this)
            .context(this)
            .build()
    }

    private class Logger : NfcLogger.Listener {
        override fun v(tag: String, message: String) {
            Log.v(tag, message)
        }

        override fun e(tag: String, throwable: Throwable) {
            Log.e(tag, throwable)
        }

        override fun d(tag: String, message: String) {
            Log.d(tag, message)
        }

        override fun i(tag: String, message: String) {
            Log.i(tag, message)
        }

        override fun w(tag: String, message: String) {
            Log.w(tag, message)
        }

        override fun e(tag: String, message: String) {
            Log.e(tag, message)
        }

    }
}