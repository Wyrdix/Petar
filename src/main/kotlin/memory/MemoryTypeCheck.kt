package fr.univ_lille.iut_info.memory

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.memory.MemoryObject.Companion.asMap

fun Type.check(fields: Any?): Boolean {
    if (fields == null) return false

    if (this is AnyType) return true
    if (fields is MemoryElement) return this.check(fields.rawValue)

    if (this is PrimitiveType.StringType) return MemoryString.asString(fields) != null
    if (this is PrimitiveType.NumberType) return MemoryNumber.asNumber(fields) != null
    if (this is PrimitiveType.BooleanType) return MemoryBoolean.asBoolean(fields) != null
    if (this is PropertyType) {

        val providedFieldsRaw = asMap(fields) ?: return false

        val expectedFieldsType = this.childrenMap
        if (providedFieldsRaw.keys.isEmpty()) return expectedFieldsType.isEmpty()
        if (providedFieldsRaw.keys.contains(null)) throw TypeCheckWrongAnyType("Map keys contains null")
        if (providedFieldsRaw.keys.find { it !is String } != null) throw TypeCheckWrongAnyType("Map keys contains a non-string key")

        @Suppress("UNCHECKED_CAST") val providedFields: Map<String, Any> = providedFieldsRaw as Map<String, Any>

        if (providedFields.keys != expectedFieldsType.keys) return false

        return expectedFieldsType.filterNot { (key, value) -> value.check(providedFields[key]) }.isEmpty()
    }
    if (this is ArrayType) {
        if (fields is Array<*>) return fields.filterNot { this.type.check(it) }.isEmpty()
        if (fields is List<*>) return fields.filterNot { this.type.check(it) }.isEmpty()
        return false
    }
    if (this is ReferenceType) {
        return this.cache?.check(fields) ?: throw TypeCheckReferenceNotCached()
    }
    throw TypeCheckWrongAnyType("Type ${fields.javaClass.simpleName} is not supported by the type checker")
}

fun Type.safeCheck(fields: Any?): Boolean {
    return try {
        check(fields)
    } catch (e: Error) {
        e.printStackTrace()
        false
    }
}

fun Type.assert(fields: Any?) {
    assert(safeCheck(fields)) {}
}

class TypeCheckWrongAnyType(message: String?) :
    Error(if (message != null) "An object was passed that is not supported by the type checker : $message." else "An object was passed that is not supported by the type checker.")

class TypeCheckReferenceNotCached :
    Error("Type check cannot occurs if reference types are not cached inside reference.")
