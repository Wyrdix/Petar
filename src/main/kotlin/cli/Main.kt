package fr.univ_lille.iut_info.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import fr.univ_lille.iut_info.Program
import fr.univ_lille.iut_info.parsing.SpecificationParser

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

    val input = command.input

    if (input != null && input.extension != "json") {
        println("The input file is not a json file.")
        return
    }

    if (input != null && !input.exists()) {
        println("The input file do not exist.")
    }


    val parsed = command.specifications.map { file ->
        val input = file.readLines().joinToString(separator = "\n")
        val (errors, statements) = SpecificationParser.parse(input)
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
    val program = Program(statements)

    program.compile().let { errors ->
        if (errors.isNotEmpty()) {
            errors.forEach { println(it) }
            return
        }
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

    if (input == null) return

    println("Parsing input file : $input.")
    val jsonObject = JsonParser.parseString(input.readLines().joinToString(separator = "")).asJsonObject

    println("Transforming file $input :")
    val evaluation = program.evaluate(jsonObject).let { (errors, evaluation) ->
        if (errors.isNotEmpty()) {
            errors.forEach { println(it) }
            return
        }
        evaluation!!
    }

    val output = command.output
    if (output != null) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        output.writeText(gson.toJson(evaluation.toJson()))
        println("Result was written in $output.")
    } else {
        println(evaluation)
    }
}