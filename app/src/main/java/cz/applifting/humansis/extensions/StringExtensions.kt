package cz.applifting.humansis.extensions

fun String?.equalsIgnoreEmpty(other: String?): Boolean {
    return this.orNullIfEmpty() == other.orNullIfEmpty()
}

fun String?.orNullIfEmpty(): String? {
    return this?.let { if (it.isEmpty()) null else it }
}
