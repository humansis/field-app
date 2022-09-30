package cz.applifting.humansis.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.applifting.humansis.model.api.NationalCardId

class NationalCardIdConverter {
    @TypeConverter
    fun toList(value: String): List<NationalCardId> {
        val listType = object : TypeToken<List<NationalCardId>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(list: List<NationalCardId>): String {
        return Gson().toJson(list)
    }
}