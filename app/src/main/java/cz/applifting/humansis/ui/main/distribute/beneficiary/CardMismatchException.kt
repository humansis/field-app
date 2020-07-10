package cz.applifting.humansis.ui.main.distribute.beneficiary

import java.lang.Exception

class CardMismatchException(val newCard: String?): Exception("Card mismatch") {
}