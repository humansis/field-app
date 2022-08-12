package cz.applifting.humansis.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.applifting.humansis.model.CommodityType

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
class CommodityTypeConverter {

    @TypeConverter
    fun toList(value: String): List<CommodityType> {
        val listType = object : TypeToken<List<CommodityType>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(list: List<CommodityType>): String {
        return Gson().toJson(list)
    }
}