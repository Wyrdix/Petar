package fr.univ_lille.iut_info

interface Transform;

class DeleteTransform(): Transform

class ExpressionTransform(val expression: Expression): Transform

class ObjectTransform(val fields: Map<String, Transform>, val children: ChildrenTransform? = null) :
    Transform

class ChildrenTransform(val transforms: List<Transform>) : Transform

class ListTransform(val transforms: List<Transform>): Transform