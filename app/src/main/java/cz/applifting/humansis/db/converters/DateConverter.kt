package cz.applifting.humansis.db.converters

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.*


/**
 * Converter used to store date as long in db.
 *
 */
class DateConverter {

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toLong(value: Date?): Long? {
        return value?.time
    }

    fun stringToDate(value: String?): Date? {
        val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return value?.let { format.parse(it) }
    }
}