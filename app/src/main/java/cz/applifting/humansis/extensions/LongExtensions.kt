package cz.applifting.humansis.extensions

private const val SECONDS_IN_MINUTE = 60
private const val MILLISECONDS_IN_SECOND = 1000

fun Long.minutesToSeconds(): Long {
    return this * SECONDS_IN_MINUTE
}

fun Long.secondsToMilliseconds(): Long {
    return this * MILLISECONDS_IN_SECOND
}