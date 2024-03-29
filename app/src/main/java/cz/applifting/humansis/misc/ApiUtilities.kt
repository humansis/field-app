package cz.applifting.humansis.misc

import cz.applifting.humansis.BuildConfig

fun isPositiveResponseHttpCode(code: Int): Boolean {
    // The positive http code is in format of 2xx
    return (code - 200 >= 0) && (code - 300 < 0)
}

fun getDefaultEnvironment(): ApiEnvironment {
    return if (BuildConfig.DEBUG) ApiEnvironment.Stage else ApiEnvironment.Prod
}
