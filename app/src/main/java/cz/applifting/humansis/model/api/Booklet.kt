package cz.applifting.humansis.model.api

data class Booklet(
    val id: Int,
    val code: String,
    val currency: String,
    val status: Int,
    val voucherValues: List<Int>
)

data class BookletsApiEntity(
    val totalCount: Int,
    val data: List<Booklet>
)