package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.TextualRangeLocated

enum class Step {
    NAME,
    TYPE,
    EVALUATION
}

class StepError(val step: Step, val range: TextualRangeLocated, override val message: String) : Error(message) {
}