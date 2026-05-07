import fr.univ_lille.iut_info.MemoryElement
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

fun assertIsSimilarTo(
    expected: MemoryElement,
    actual: MemoryElement,
    context: ITypingContext?,
    message: String? = null
) {
    asserter.isSimilarTo(message, expected, actual, context)
}