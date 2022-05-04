package cz.applifting.humansis.model.api

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 29, September, 2019
 */
data class ReliefPackage(
    val id: Int,
    val modalityType: ModalityType,
    val amountToDistribute: Double,
    val amountDistributed: Double,
    val unit: String,
    val notes: String
)