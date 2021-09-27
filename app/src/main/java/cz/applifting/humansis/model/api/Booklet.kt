package cz.applifting.humansis.model.api

data class Booklet(
    val id: Int,
    val code: String,
    val currency: String,
    val status: Int,
    val vouchers: List<Voucher> // TODO tohle potrebuju
    // TODO dát z endpointu pryč totalValue, individualValues, quantityOf..., distributed
)