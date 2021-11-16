package cz.applifting.humansis.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.applifting.humansis.model.db.CommodityLocal

/**
 * Converter used to store commodities as json.
 *
 */
class CommodityConverter {

    @TypeConverter
    fun toList(value: String): List<CommodityLocal> {
        val listType = object : TypeToken<List<CommodityLocal>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(list: List<CommodityLocal>): String {
        return Gson().toJson(list)
    }
}