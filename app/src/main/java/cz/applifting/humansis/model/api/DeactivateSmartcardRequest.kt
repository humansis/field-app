package cz.applifting.humansis.model.api

import cz.applifting.humansis.misc.SmartcardState

data class DeactivateSmartcardRequest (
        val state: String = SmartcardState.STATE_DEACTIVATED.state,
        val createdAt: String
)