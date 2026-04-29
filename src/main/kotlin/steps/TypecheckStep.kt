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

        val declarationTypeError = types.map {
            try {
                it.type.check(this)
                return@map true
            } catch (e: IllegalStateException) {
                val message = e.message
                if (message != null) errorList.add(message)
                return@map false
            }
        }.contains(false)

        if (declarationTypeError) errorList.add("FatalError : Type error found in property type declaration")

        val ruleTypeError = rules.map {
            try {
                val leftSynthesized = it.pattern.typeSynthesis(this)
                val rightSynthesized = it.production.typeSynthesis(this)

                return@map (leftSynthesized != null && leftSynthesized !is BottomType) && (rightSynthesized != null && rightSynthesized !is BottomType)
            } catch (e: IllegalStateException) {
                val message = e.message
                if (message != null) errorList.add(message)
                return@map false
            }
        }.contains(false)
        if (ruleTypeError) errorList.add("FatalError : Type error found in annotation rules.")


        return errorList
    }
}