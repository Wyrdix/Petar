package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.TextualRangeLocated
import java.io.File
import java.util.Locale.getDefault
import kotlin.math.ceil
import kotlin.math.log10

enum class Step {
    NAME,
    TYPE,
    EVALUATION
}

class StepError(val step: Step, val range: TextualRangeLocated, override val message: String) : Error(message) {
    fun printFormated(fileMap: Map<String, File>) {
        val range = range.textual
        val message = message
        val errorType = step.name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() } + "Error"
        if (range == null || !fileMap.containsKey(range.filename)) {
            System.err.println("${errorType}: $message")
        } else {
            val file = fileMap[range.filename]!!

            val padding = ceil(log10((range.end.line + 1).toDouble())).toInt()

            println(padding)

            val lines = file.readLines().subList(range.begin.line, range.end.line + 1)
                .mapIndexed { index, string ->
                    val size =
                        if (index + range.begin.line == 0) 0 else log10((index + range.begin.line).toDouble()).toInt()
                    "${index + range.begin.line}${" ".repeat(padding - size)}| $string"
                }
            val linesJoined = lines.joinToString(separator = "\n") { it }

            var localization: String = ""
            if (range.end.line == range.begin.line) {
                localization =
                    "\n${" ".repeat(range.begin.row + padding + 3)}${"^".repeat(range.end.row - range.begin.row + 1)}"
            }

            System.err.println(
                "$errorType in ${range.filename} (${range.begin.line}:${range.begin.row} to ${range.end.line}:${range.end.row}): ${message}\n${linesJoined}${localization}"
            )
        }
    }
}