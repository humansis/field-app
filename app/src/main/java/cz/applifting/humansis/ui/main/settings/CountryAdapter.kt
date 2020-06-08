package cz.applifting.humansis.ui.main.settings

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import cz.applifting.humansis.R

class CountryAdapter(context: Context) : SpinnerAdapter, ArrayAdapter<String>(context, R.layout.item_country) {

    fun setData(data: List<String>) {
        setDropDownViewResource(R.layout.item_country_dropdown)

        clear()
        addAll(data)
        notifyDataSetChanged()
    }
}