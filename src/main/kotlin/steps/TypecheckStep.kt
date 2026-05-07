package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

class TypecheckStep(override val nameContext: NameStep) : ExecutionStep, ITypingContext, INameContext by nameContext {

    override val propertyResolved: HashMap<PropertyType, Map<String, Type>> = HashMap()
    override val expressionSynthesized: HashMap<Expression, Type> = HashMap()
    override val expressionChecked: HashMap<Expression, Type> = HashMap()
    override val patternSynthesized: HashMap<Pattern, Type> = HashMap()
    override val patternChecked: HashMap<Pattern, Type> = HashMap()

    override fun run(): List<String> {

        val errorList: MutableList<String> = ArrayList()

        val types = program.statements.filterIsInstance<PropertyDeclarationStatement>()
        val rules = program.statements.filterIsInstance<ProductionRuleStatement>()

        types.forEach {
            try {
                it.type.check(this)
            } catch (e: IllegalStateException) {
                val message = e.message
                throw StepError(Step.TYPE, it.type, message ?: "Unknown Error")
            }
        }

        rules.forEach {
            try {
                val leftSynthesized = it.pattern.typeSynthesis(this)
                val rightSynthesized = it.production.typeSynthesis(this)

                if ((leftSynthesized ?: Type.bottom) == Type.bottom) throw StepError(
                    Step.TYPE,
                    it,
                    "Unknown error in pattern"
                )
                if ((rightSynthesized
                        ?: Type.bottom) == Type.bottom
                ) {
                    throw StepError(
                        Step.TYPE,
                        it,
                        "Unknown error in production"
                    )
                }
            } catch (e: IllegalStateException) {
                val message = e.message
                throw StepError(Step.TYPE, it, message ?: "Unknown Error")
            }
        }

        return errorList
    }
}