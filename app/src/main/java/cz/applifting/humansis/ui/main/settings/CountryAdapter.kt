package cz.applifting.humansis.ui.main.settings

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import cz.applifting.humansis.R
import cz.applifting.humansis.model.Country

class CountryAdapter(context: Context) : SpinnerAdapter, ArrayAdapter<Country>(context, R.layout.item_country) {

    fun setData(data: List<Country>) {
        setDropDownViewResource(R.layout.item_country_dropdown)

        clear()
        addAll(data)
        notifyDataSetChanged()
    }
}