package cz.applifting.humansis.misc

object ApiUtilities {

    fun isPositiveResponseHttpCode(code: Int): Boolean {
        // The positive http code is in format of 2xx
        return (code - 200 >= 0) && (code - 300 < 0)
    }
}