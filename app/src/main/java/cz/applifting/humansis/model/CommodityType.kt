package cz.applifting.humansis.model

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.R

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 28, August, 2019
 */
enum class CommodityType(val drawableResId: Int) {
    @SerializedName("Cash") CASH(R.drawable.ic_commodity_cash),
    @SerializedName("Loan") LOAN(R.drawable.ic_commodity_loan),
    @SerializedName("NFI Kit") NFI_KIT(R.drawable.ic_commodity_nfi_kit),
    @SerializedName("RTE Kit") RTE_KIT(R.drawable.ic_commodity_rte_kit),
    @SerializedName("Ready to Eat Rations") READY_TO_EAT_RATIONS(R.drawable.ic_commodity_rte_kit),
    @SerializedName("Paper Voucher") PAPER_VOUCHER(R.drawable.ic_commodity_voucher),
    @SerializedName("Food") FOOD(R.drawable.ic_commodity_food),
    @SerializedName("Food Rations") FOOD_RATIONS(R.drawable.ic_commodity_food),
    @SerializedName("QR Code Voucher") QR_VOUCHER(R.drawable.ic_commodity_voucher),
    @SerializedName("Bread") BREAD(R.drawable.ic_commodity_bread),
    @SerializedName("Agricultural Kit") AGRICULTURAL_KIT(R.drawable.ic_commodity_agricultural_kit),
    @SerializedName("WASH Kit") WASH_KIT(R.drawable.ic_commodity_wash_kit),
    @SerializedName("Shelter tool kit") SHELTER_TOOL_KIT(R.drawable.ic_commodity_shelter),
    @SerializedName("Hygiene kit") HYGIENE_KIT(R.drawable.ic_commodity_hygiene_kit),
    @SerializedName("Dignity kit") DIGNITY_KIT(R.drawable.ic_commodity_dignity),
    @SerializedName("Smartcard") SMARTCARD(R.drawable.ic_smartcard),
    @SerializedName("Business Grant") BUSINESS_GRANT(R.drawable.ic_commodity_unknown), //TODO add drawable
    @SerializedName("Winterization kit") WINTERIZATION_KIT(R.drawable.ic_commodity_unknown), //TODO add drawable
    @SerializedName("Mobile Money") MOBILE_MONEY(0),
    UNKNOWN(R.drawable.ic_commodity_unknown)
}