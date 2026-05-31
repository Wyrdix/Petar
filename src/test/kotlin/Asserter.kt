import fr.univ_lille.iut_info.memory.MemoryElement
import fr.univ_lille.iut_info.steps.EvaluationEnvironment
import fr.univ_lille.iut_info.steps.IEvaluatingContext
import fr.univ_lille.iut_info.steps.ITypingContext
import kotlin.test.Asserter
import kotlin.test.asserter


internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

fun Asserter.isSimilarTo(message: String?, expected: MemoryElement?, actual: MemoryElement?, context: ITypingContext?) {
    assertTrue(
        { messagePrefix(message) + "Expected <$expected>, actual <$actual>." },
        expected != null && actual != null && expected.isSimilarTo(actual, context)
    )
}

fun Asserter.isSimilarTo(message: String?, expected: List<EvaluationEnvironment>, actual: List<EvaluationEnvironment>, context: IEvaluatingContext?) {
    assertTrue(
        { messagePrefix(message) + "Expected <$expected>, actual <$actual>." },
        expected.size == actual.size && expected.zip(actual).all {( expected, actual) ->

            expected.definitions.size == actual.definitions.size && expected.definitions.keys.containsAll(actual.definitions.keys) &&
                    expected.definitions.keys.all { expected.definitions[it]!!.isSimilarTo(actual.definitions[it]!!, context) }

            expected.choices.size == actual.choices.size && expected.choices.keys.containsAll(actual.choices.keys) &&
                    expected.choices.keys.all { expected.choices[it]!!.isSimilarTo(actual.choices[it]!!, context) }

        }
    )
}

fun assertIsSimilarTo(
    expected: MemoryElement,
    actual: MemoryElement,
    context: ITypingContext?,
    message: String? = null
) {
    asserter.isSimilarTo(message, expected, actual, context)
}

fun assertIsSimilarTo(
    expected: List<EvaluationEnvironment>,
    actual: List<EvaluationEnvironment>,
    context: IEvaluatingContext?,
    message: String? = null
) {
    asserter.isSimilarTo(message, expected, actual, context)
}