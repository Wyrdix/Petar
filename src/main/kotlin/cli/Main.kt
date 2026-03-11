package fr.univ_lille.iut_info.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import fr.univ_lille.iut_info.name.NameAnalysis
import fr.univ_lille.iut_info.parsing.Parser

fun main(args: Array<String>) {
    val command = Command()
    val commander: JCommander = JCommander.newBuilder().addObject(command).build()
    commander.programName = "alfr"
    try {
        commander.parse(*args)
    } catch (_: ParameterException) {
        commander.usage()
        return
    }

    if (command.help) {
        commander.usage()
        return
    }

    val parsed = command.specifications.map { file ->
        val input = file.readLines().joinToString(separator = "\n")
        val (errors, statements) = Parser.parse(input)
        Triple(file, errors, statements)
    }.associateBy(
        { (file, _, _) -> file.absolutePath },
        { (_, strings, statements) -> Pair(strings, statements) })

    if (parsed.values.find { (strings, _) -> strings.isNotEmpty() } != null) {
        parsed.filter { entry -> entry.value.first.isNotEmpty() }.forEach { path, (errors, _) ->
            println("Error in file : $path")
            errors.forEach { println(it) }
        }
        return
    }

    val statements = parsed.flatMap { it.value.second }

    val nameErrors = NameAnalysis(statements).check()

    if(nameErrors.isNotEmpty()) {
        nameErrors.forEach { println(it) }
        return
    }

    if (command.printSpecification) {
        if (command.specifications.isEmpty()) {
            println("No specifications were provided.")
        } else {
            parsed.forEach { file, (_, statements) ->
                println("Specification file : $file")
                statements.forEach { println(it) }
                println()
            }
        }
    }
}