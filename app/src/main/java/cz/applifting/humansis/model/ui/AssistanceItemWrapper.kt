package cz.applifting.humansis.model.ui

import cz.applifting.humansis.model.db.AssistanceLocal

data class AssistanceItemWrapper(
    val assistance: AssistanceLocal,
    val numberOfReachedBeneficiaries: Int
)