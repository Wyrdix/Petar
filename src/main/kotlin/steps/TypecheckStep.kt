package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

class TypecheckStep(override val nameContext: NameStep) : ExecutionStep, ITypingContext, INameContext by nameContext {

    override val propertyResolved: HashMap<PropertyType, Map<String, Type>> = HashMap()
    override val expressionSynthesized: HashMap<Expression, Type> = HashMap()
    override val expressionChecked: HashMap<Expression, Type> = HashMap()
    override val patternSynthesized: HashMap<Pattern, Type> = HashMap()
    override val patternChecked: HashMap<Pattern, Type> = HashMap()

    override fun typeSynthesis(exp: Expression, type: Type): Type {
        expressionSynthesized[exp] = type
        return type
    }

    override fun typePatternSynthesis(pattern: Pattern, type: Type?): Type? {
        if (type != null) patternSynthesized[pattern] = type
        return type
    }

    override fun typeChecked(exp: Expression, condition: Boolean, type: Type): Boolean {
        if (condition) {
            val alreadyChecked = expressionChecked[exp]
            if (alreadyChecked != null) {

                if (!alreadyChecked.isAssignableFrom(this, type)) expressionChecked[exp] = type
                else if (!type.isAssignableFrom(this, alreadyChecked)) {
                    expressionChecked[exp] = Type.bottom
                    return false
                }
            } else expressionChecked[exp] = type
        }
        return condition
    }

    override fun typePatternChecked(pattern: Pattern, condition: Boolean, type: Type): Boolean {
        if (condition) {
            val alreadyChecked = patternChecked[pattern]
            if (alreadyChecked != null) {

                if (!alreadyChecked.isAssignableFrom(this, type)) patternChecked[pattern] = type
                else if (!type.isAssignableFrom(this, alreadyChecked)) {
                    patternChecked[pattern] = Type.bottom
                    return false
                }
            } else patternChecked[pattern] = type
        }
        return condition
    }

    override fun getSynthesizedType(exp: Expression): Type? {
        return expressionSynthesized[exp]
    }

    override fun getPatternSynthesizedType(pattern: Pattern): Type? {
        return patternSynthesized[pattern]
    }

    override fun getCheckedType(exp: Expression): Type? {
        return expressionChecked[exp]
    }

    override fun getCheckedPatternType(pattern: Pattern): Type? {
        return patternChecked[pattern]
    }

    override fun getType(name: String): Type {
        return typeNameMap[name] ?: throw IllegalStateException("Could not find type by name: $name.")
    }


    val program
        get() = nameContext.program

    override fun run(): List<String> {

        val errorList: MutableList<String> = ArrayList()

        val types = program.statements.filterIsInstance<PropertyDeclarationStatement>()
        val rules = program.statements.filterIsInstance<ProductionRuleStatement>()

        val ruleTypeError = rules.map {
            try {
                val leftSynthesized = it.pattern.typeSynthesis(this)
                val rightSynthesized = it.production.typeSynthesis(this)

                return@map !((leftSynthesized != null && leftSynthesized !is BottomType) && (rightSynthesized != null && rightSynthesized !is BottomType))
            } catch (e: Exception) {
                val message = e.message
                if (message != null) errorList.add(message)
                return@map false
            }
        }.contains(false)
        if (ruleTypeError) errorList.add("FatalError : Type error found in annotation rules.")

        val declarationTypeError = types.map {
            try {
                it.type.check(this)
                return@map true
            } catch (e: Exception) {
                val message = e.message
                if (message != null) errorList.add(message)
                return@map false
            }
        }.contains(false)

        if (declarationTypeError) errorList.add("FatalError : Type error found in property type declaration")


        return errorList
    }
}