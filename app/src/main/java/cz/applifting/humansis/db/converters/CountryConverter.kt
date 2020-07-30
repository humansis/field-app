package cz.applifting.humansis.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.applifting.humansis.model.Country

class CountryConverter {

    @TypeConverter
    fun toList(value: String): List<Country> {
        val listType = object : TypeToken<List<Country>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(list: List<Country>): String {
        return Gson().toJson(list)
    }
}