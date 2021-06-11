package cz.applifting.humansis.db.converters

import androidx.room.TypeConverter
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
}