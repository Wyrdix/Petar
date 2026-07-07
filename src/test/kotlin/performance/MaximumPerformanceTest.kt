package performance

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.memory.MemoryNumber
import fr.univ_lille.iut_info.memory.MemoryObject
import fr.univ_lille.iut_info.memory.MemoryReference
import fr.univ_lille.iut_info.parsing.SpecificationParser
import fr.univ_lille.iut_info.steps.StepError
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.stream.Stream
import kotlin.system.exitProcess
import kotlin.test.assertEquals

class MaximumPerformanceTest {

    sealed interface Node : Visitable<Node>
    data class Binary(val left: Node, val right: Node) : Node {
        override fun accept(visitor: Visitor<Node>): Node {
            return Binary(visitor.visit(left), visitor.visit(right))
        }

    }

    data class Leaf(val value: Number) : Node {
        override fun accept(visitor: Visitor<Node>): Node {
            return this
        }
    }


    val file = File("src/test/kotlin/performance/maximum_performance.petar")

    val createProgram = { tree: Node ->
        file.let {
            val input = file.readLines().joinToString(separator = "\n")
            val (errors, statements) = SpecificationParser.parse("maximum_performance", input)

            println("Error in file : maximum_performance")
            errors.forEach { println(it) }
            if (errors.isNotEmpty()) exitProcess(1)

            var data = ProgramData(statements) {

                tree.map { node, rec ->
                    when (node) {
                        is Binary -> MemoryObject(
                            it.typeNameMap["Binary"] as PropertyType,
                            mapOf("left" to rec(node.left), "right" to rec(node.right))
                        )

                        is Leaf -> MemoryObject(
                            it.typeNameMap["Leaf"] as PropertyType, mapOf("value" to MemoryNumber(node.value))
                        )
                    }
                }
            }

            Program(data).apply {
                compile()
            }
        }
    }


    @TestFactory
    fun depths(): Stream<DynamicTest> {

        return (0..6).map { i ->
            val name = "depth_$i"
            DynamicTest.dynamicTest(
                name
            ) {
                var toBeIncremented = 0;
                var build: ((Int) -> Node)? = null;
                build = { level: Int ->
                    if (level == 0) Leaf(toBeIncremented++)
                    else Binary(build!!(level - 1), build!!(level - 1))
                }

                try {

                    val startTime = System.nanoTime()
                    val program = createProgram(build(i))
                    val evaluate = program.evaluate()!!
                    val rootMinimum = program.evaluate.getAnnotations(evaluate)
                        .firstOrNull() { it.type.toString() == "Maximum" }!! as MemoryObject
                    val minimumValue =
                        ((rootMinimum.value["value"] as MemoryReference).reference.resolve() as MemoryNumber).value
                    val endTime = System.nanoTime();

                    assert((endTime - startTime) < 1e9)
                    println("Time elapsed: ${(endTime - startTime) / 1e9}s")
                    assertEquals(minimumValue, toBeIncremented - 1)
                } catch (e: StepError) {
                    e.printFormated(mapOf("maximum_performance" to file))
                    throw e;
                }
            }
        }.stream()
    }
}