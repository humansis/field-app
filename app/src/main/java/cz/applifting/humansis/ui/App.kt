package cz.applifting.humansis.ui

import android.app.Application
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.R
import cz.applifting.humansis.di.AppComponent
import cz.applifting.humansis.di.DaggerAppComponent
import cz.quanti.android.nfc.logger.NfcLogger
import quanti.com.kotlinlog.Log
import quanti.com.kotlinlog.android.AndroidLogger
import quanti.com.kotlinlog.android.MetadataLogger
import quanti.com.kotlinlog.base.LogLevel
import quanti.com.kotlinlog.base.LoggerBundle
import quanti.com.kotlinlog.file.FileLogger
import quanti.com.kotlinlog.file.bundle.CircleLogBundle

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */

class App : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        initLogger()

        appComponent = DaggerAppComponent.builder()
            .app(this)
            .context(this)
            .build()
    }

    private fun initLogger() {
        Log.initialise(this)
        Log.addLogger(AndroidLogger(LoggerBundle(LogLevel.DEBUG)))
        val circleLogBundle = CircleLogBundle(LogLevel.VERBOSE, 1, 50, 30, LogLevel.VERBOSE )
        Log.addLogger(FileLogger(applicationContext, circleLogBundle))
        Log.useUncheckedErrorHandler()
        NfcLogger.registerListener(Logger())
        MetadataLogger.customMetadataLambda = {
            val metadata: MutableList<Pair<String, String>> = mutableListOf()
            metadata.add(Pair("VERSION:", getString(R.string.version, BuildConfig.VERSION_NAME)))
            metadata.add(Pair("BUILD_NUMBER:", BuildConfig.BUILD_NUMBER.toString()))
            metadata
        }
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

    companion object {
        private val TAG = App::class.java.simpleName
    }
}