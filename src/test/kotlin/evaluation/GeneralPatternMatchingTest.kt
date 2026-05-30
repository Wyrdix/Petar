package evaluation

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.memory.*
import fr.univ_lille.iut_info.memory.MemoryPath
import fr.univ_lille.iut_info.steps.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GeneralPatternMatchingTest {
    @Test
    fun simpleProperty() {
        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))

        val type = Type.property("test", mapOf("a" to Type.number))
        val value = MemoryObject(
            type,
            mapOf("a" to MemoryNumber(1))
        )

        context.initial(value, MemoryPath.root(context))

        println(context.pathMemory.keys)

        context.typeNameMap["test"] = type

        assertEquals(
            IIterator.singleton(EvaluationEnvironment()).toList(),
            PropertyPattern("test", emptyList(), meta = PatternMeta()).match(context, value).toList()
        )

        assertEquals(
            IIterator.singleton(EvaluationEnvironment(definitions = mapOf("outer" to value))).toList(),
            PropertyPattern("test", emptyList(), meta = PatternMeta(name = "outer")).match(context, value).toList()
        )

        assertEquals(
            IIterator.singleton(EvaluationEnvironment(definitions = mapOf("outer" to value))).toList(), PropertyPattern(
                "test",
                listOf("a" to ExpressionPattern(LiteralExpression.ENumber(1f), meta = PatternMeta())),
                meta = PatternMeta(name = "outer")
            ).match(context, value).toList()
        )

        assertEquals(
            emptyList(), PropertyPattern(
                "test",
                listOf("a" to ExpressionPattern(LiteralExpression.ENumber(2f), meta = PatternMeta())),
                meta = PatternMeta(name = "outer")
            ).match(context, value).toList()
        )


        assertEquals(
            IIterator.singleton(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "outer" to value, "inner" to value.value["a"]!!
                    )
                )
            ).toList(), PropertyPattern(
                "test",
                listOf("a" to ExpressionPattern(LiteralExpression.ENumber(1f), meta = PatternMeta(name = "inner"))),
                meta = PatternMeta(name = "outer")
            ).match(context, value).toList()
        )
    }

    @Test
    fun regex() {
        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))
        val value = MemoryString("Hello World !")

        assertEquals(
            IIterator.singleton(EvaluationEnvironment()).toList(),
            RegexPattern("H.*", meta = PatternMeta()).match(context, value).toList()
        )

        assertEquals(
            emptyList(), RegexPattern("z.*", meta = PatternMeta()).match(context, value).toList()
        )


        assertEquals(
            IIterator.singleton(EvaluationEnvironment()).toList(),
            RegexPattern(".*o.*o.*", meta = PatternMeta()).match(context, value).toList()
        )
    }

    @Test
    fun throughAnnotation1() {

        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))

        val type = Type.property("test", mapOf("a" to Type.number))
        val annotationType = Type.property("name_annotation", mapOf("name" to Type.string))
        val valueField = MemoryNumber(1)
        val value = MemoryObject(type, mapOf("a" to valueField))
        val annotationValue = MemoryObject(
            annotationType,
            mapOf("name" to MemoryNumber(2))
        )

        context.initial(value, MemoryPath.root(context))

        context.addAnnotation(valueField, annotationValue)

        context.typeNameMap["test"] = type
        context.typeNameMap["name_annotation"] = annotationType

        assertEquals(
            IIterator.singleton(EvaluationEnvironment(choices = mapOf(valueField to annotationValue))).toList(), PropertyPattern(
                "test",
                listOf("a" to PropertyPattern("name_annotation", emptyList(), meta = PatternMeta())),
                meta = PatternMeta()
            ).match(context, value).toList()
        )

    }

    @Test
    fun throughAnnotation2() {

        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))

        val type = Type.property("test", mapOf("a" to Type.number))
        val annotationType = Type.property("name_annotation", mapOf("name" to Type.string))
        val annotation2Type = Type.property("value_annotation", mapOf("b" to Type.string))
        val valueField = MemoryNumber(1)
        val value = MemoryObject(type, mapOf("a" to valueField))
        val annotationValue = MemoryObject(
            annotationType,
            mapOf("name" to MemoryNumber(2))
        )
        val annotation2Value = MemoryObject(
            annotation2Type,
            mapOf("b" to MemoryNumber(3))
        )

        context.initial(value, MemoryPath.root(context))

        context.addAnnotation(valueField, annotationValue)
        context.addAnnotation(annotationValue.value["name"]!!, annotation2Value)


        context.typeNameMap["test"] = type
        context.typeNameMap["name_annotation"] = annotationType
        context.typeNameMap["value_annotation"] = annotation2Type

        assertEquals(
            listOf(EvaluationEnvironment(choices = mapOf(valueField to annotationValue, annotationValue.value["name"]!! to annotation2Value))),  PropertyPattern(
                "test", listOf(
                    "a" to PropertyPattern(
                        "name_annotation",
                        listOf("name" to PropertyPattern("value_annotation", emptyList(), meta = PatternMeta())),
                        meta = PatternMeta()
                    )
                ), meta = PatternMeta()
            ).match(context, value).toList()
        )
    }
}