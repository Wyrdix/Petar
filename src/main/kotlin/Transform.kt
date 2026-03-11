package fr.univ_lille.iut_info

interface Transform;

class DeleteTransform() : Transform {
    override fun equals(other: Any?): Boolean {
        return other != null && other is DeleteTransform
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "DeleteTransform()"
    }
}

data class ExpressionTransform(val expression: Expression) : Transform

data class ObjectTransform(val fields: List<Pair<String, Transform>>, val children: ChildrenTransform? = null) :
    Transform {
    val fieldsMap: Map<String, Transform>
        get() = fields.associateBy({ it.first }, { it.second })
}

data class ChildrenTransform(val transforms: List<Transform>) : Transform

data class ListTransform(val transforms: List<Transform>) : Transform