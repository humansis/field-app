package cz.applifting.humansis.model

data class Country(
    val iso3: String = "",
    var name: String = ""
) {
    override fun toString(): String {
        return name
    }
}