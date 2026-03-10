package fr.univ_lille.iut_info.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import fr.univ_lille.iut_info.Parser

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

    if (command.printSpecification) {
        if (command.specifications.isEmpty()) {
            println("No specifications were provided.")
        } else {
            val statements =
                command.specifications.map { file -> file.readLines().joinToString(separator = "\n") { it } }
                    .flatMap { Parser.parse(it) }

            statements.forEach { println(it) }
        }
    }
}