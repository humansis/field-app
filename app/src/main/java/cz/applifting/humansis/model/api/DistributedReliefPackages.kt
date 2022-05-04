package cz.applifting.humansis.model.api

data class DistributedReliefPackages(
    val id: Int,
    val dateDistributed: String,
    // Leave amountDistributed null or enter whole amountToDistribute if relief was distributed completely.
    val amountDistributed: Double? = null // TODO for future usages, such as partial distributions.
)