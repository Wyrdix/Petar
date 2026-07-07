package evaluation

import assertIsSimilarTo
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.steps.*
import org.junit.jupiter.api.Test

class ArrayPatternMatchingTest {
    @Test
    fun simple() {
        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))
        val generator: (meta: PatternMeta) -> Pattern = { meta ->
            val pattern = PatternNesting(regexPattern("[a-zA-Z]*"), meta)
            context.patternNodeMap[pattern] = NameNode(null)
            context.patternNodeMap[pattern.pattern] = NameNode(context.patternNodeMap[pattern])
            val typeCheck = pattern.typeCheck(context, Type.string, true)
            if (!typeCheck) println("Generator failed : $meta")
            pattern
        }

        val elements = listOf("aaa").map { _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryString(it) }

        val arrayPattern = ArrayPattern(
            listOf(generator(PatternMeta(modifier = PatternModifier.AT_LEAST_ONE, name = "group1"))),
            meta = PatternMeta()
        )

        initial(context, arrayPattern)
        fillNodes(context, arrayPattern)
        arrayPattern.typeCheck(context, Type.array(Type.string))

        assertIsSimilarTo(
            listOf(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), listOf(elements[0])
                        )
                    )
                )
            ), arrayMatching(
                context, arrayPattern.values, elements
            ).toList(), context
        )
    }

    @Test
    fun multiple_2() {
        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))
        val generator: (meta: PatternMeta) -> Pattern = { meta ->
            val pattern = PatternNesting(regexPattern("[a-zA-Z]*"), meta)
            context.patternNodeMap[pattern] = NameNode(null)
            context.patternNodeMap[pattern.pattern] = NameNode(context.patternNodeMap[pattern])
            val typeCheck = pattern.typeCheck(context, Type.string, true)
            if (!typeCheck) println("Generator failed : $meta")
            pattern
        }

        val elements = listOf("aaa", "bbb").map { _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryString(it) }

        val arrayPattern = ArrayPattern(
            listOf(generator(PatternMeta(modifier = PatternModifier.AT_LEAST_ONE, name = "group1"))),
            meta = PatternMeta()
        )

        initial(context, arrayPattern)
        fillNodes(context, arrayPattern)
        arrayPattern.typeCheck(context, Type.array(Type.string))

        assertIsSimilarTo(
            listOf(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements
                        )
                    )
                )
            ), arrayMatching(
                context, arrayPattern.values, elements
            ).toList(), context
        )
    }

    @Test
    fun multiple() {
        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))
        val generator: (meta: PatternMeta) -> Pattern = { meta ->
            val pattern = PatternNesting(regexPattern("[a-zA-Z]*"), meta)
            context.patternNodeMap[pattern] = NameNode(null)
            context.patternNodeMap[pattern.pattern] = NameNode(context.patternNodeMap[pattern])
            val typeCheck = pattern.typeCheck(context, Type.string, true)
            if (!typeCheck) println("Generator failed : $meta")
            pattern
        }

        val elements1 = listOf("aaa", "bbb", "ccc").map {
            _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryString(
                it
            )
        }
        val elements2 = listOf("aaa", "bbb", "ccc", "ddd", "eee").map {
            _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryString(
                it
            )
        }

        val arrayPattern = ArrayPattern(
            listOf(generator(PatternMeta(modifier = PatternModifier.AT_LEAST_ONE, name = "group1"))),
            meta = PatternMeta()
        )

        initial(context, arrayPattern)
        fillNodes(context, arrayPattern)
        arrayPattern.typeCheck(context, Type.array(Type.string))

        assertIsSimilarTo(
            listOf(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements1
                        )
                    )
                )
            ), arrayMatching(
                context, arrayPattern.values, elements1
            ).toList(), context
        )

        assertIsSimilarTo(
            listOf(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements2
                        )
                    )
                )
            ), arrayMatching(
                context, arrayPattern.values, elements2
            ).toList(), context
        )
    }

    @Test
    fun choice_1() {

        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))
        val generator: (meta: PatternMeta) -> Pattern = { meta ->
            val pattern = PatternNesting(regexPattern("[a-zA-Z]*"), meta)
            context.patternNodeMap[pattern] = NameNode(null)
            context.patternNodeMap[pattern.pattern] = NameNode(context.patternNodeMap[pattern])
            val typeCheck = pattern.typeCheck(context, Type.string, true)
            if (!typeCheck) println("Generator failed : $meta")
            pattern
        }

        val elements1 = listOf("aaa", "bbb").map { _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryString(it) }

        val arrayPattern = ArrayPattern(
            listOf(
                generator(PatternMeta(modifier = PatternModifier.ANY, name = "group1")),
                generator(PatternMeta(modifier = PatternModifier.ANY, name = "group2"))
            ), meta = PatternMeta()
        )

        initial(context, arrayPattern)
        fillNodes(context, arrayPattern)
        arrayPattern.typeCheck(context, Type.array(Type.string))

        assertIsSimilarTo(
            listOf(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements1
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements1.subList(0, 1)
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements1.subList(1, 2)
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements1
                        )
                    )
                )
            ), arrayMatching(
                context, arrayPattern.values, elements1
            ).toList(), context
        )
    }


    @Test
    fun choice_2() {

        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))
        val generator: (meta: PatternMeta) -> Pattern = { meta ->
            val pattern = PatternNesting(regexPattern("[a-zA-Z]*"), meta)
            context.patternNodeMap[pattern] = NameNode(null)
            context.patternNodeMap[pattern.pattern] = NameNode(context.patternNodeMap[pattern])
            val typeCheck = pattern.typeCheck(context, Type.string, true)
            if (!typeCheck) println("Generator failed : $meta")
            pattern
        }

        val elements = listOf("aaa", "bbb", "ccc").map {
            _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryString(
                it
            )
        }

        val arrayPattern = ArrayPattern(
            listOf(
                generator(PatternMeta(modifier = PatternModifier.ANY, name = "group1")),
                generator(PatternMeta(modifier = PatternModifier.ANY, name = "group2"))
            ), meta = PatternMeta()
        )

        initial(context, arrayPattern)
        fillNodes(context, arrayPattern)
        arrayPattern.typeCheck(context, Type.array(Type.string))

        assertIsSimilarTo(
            listOf(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(0, 2)
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(2, 3)
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(0, 1)
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(1, 3)
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements
                        )
                    )
                )
            ), arrayMatching(
                context, arrayPattern.values, elements
            ).toList(), context
        )
    }

    @Test
    fun choice_complex() {

        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))
        val generator: (meta: PatternMeta, value: String) -> Pattern = { meta, value ->
            val pattern = PatternNesting(regexPattern(value), meta)
            context.patternNodeMap[pattern] = NameNode(null)
            context.patternNodeMap[pattern.pattern] = NameNode(context.patternNodeMap[pattern])
            val typeCheck = pattern.typeCheck(context, Type.string, true)
            if (!typeCheck) println("Generator failed : $meta")
            pattern
        }

        val elements = listOf("aaa", "bbb", "ccc", "bbb", "ccc").map {
            _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryString(
                it
            )
        }

        val arrayPattern = ArrayPattern(
            listOf(
                generator(PatternMeta(modifier = PatternModifier.ANY, name = "group1"), "(a*|c*)"),
                generator(PatternMeta(modifier = PatternModifier.ANY, name = "group2"), "(a*|b*|c*)"),
                generator(PatternMeta(modifier = PatternModifier.ANY, name = "group3"), "(a*|c*)")
            ), meta = PatternMeta()
        )

        initial(context, arrayPattern)
        fillNodes(context, arrayPattern)
        arrayPattern.typeCheck(context, Type.array(Type.string))

        assertIsSimilarTo(
            listOf(
                EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(0, 1)
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(1, elements.size)
                        ), "group3" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(0, 1)
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(1, elements.size - 1)
                        ), "group3" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(elements.size - 1, elements.size)
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements
                        ), "group3" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        )
                    )
                ), EvaluationEnvironment(
                    definitions = mapOf(
                        "group1" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), emptyList()
                        ), "group2" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(0, elements.size - 1)
                        ), "group3" to _root_ide_package_.fr.univ_lille.iut_info.memory.MemoryArray(
                            Type.array(Type.string), elements.subList(elements.size - 1, elements.size)
                        )
                    )
                )
            ), arrayMatching(
                context, arrayPattern.values, elements
            ).toList(), context
        )
    }
}