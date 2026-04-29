package fr.univ_lille.iut_info.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import fr.univ_lille.iut_info.MemoryObject
import fr.univ_lille.iut_info.Program
import fr.univ_lille.iut_info.ProgramData
import fr.univ_lille.iut_info.parsing.SpecificationParser
import fr.univ_lille.iut_info.serializer.JsonSerializer

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
    val rootType = command.rootType

    if (input != null && input.extension != "json") {
        println("The input file is not a json file.")
        return
    }

    if (input != null && rootType == null) {
        println("Root type is needed see help.")
        return
    }

    if (input != null && !input.exists()) {
        println("The input file do not exist.")
    }


    val parsed = command.specifications.map { file ->
        val input = file.readLines().joinToString(separator = "\n")
        val (errors, statements) = SpecificationParser.parse(input)
        Triple(file, errors, statements)
    }.associateBy({ (file, _, _) -> file.absolutePath }, { (_, strings, statements) -> Pair(strings, statements) })

    if (parsed.values.find { (strings, _) -> strings.isNotEmpty() } != null) {
        parsed.filter { entry -> entry.value.first.isNotEmpty() }.forEach { path, (errors, _) ->
            println("Error in file : $path")
            errors.forEach { println(it) }
        }
        return
    }

    val statements = parsed.flatMap { it.value.second }
    val program = Program(
        ProgramData(statements, if (rootType != null && input != null) { context ->
            println("Parsing input file : $input.")
            val json = Gson().fromJson(
                input.readLines().joinToString(separator = ""){ it }, JsonElement::class.java
            )

            val type = context.typeNameMap[rootType]
                ?: throw IllegalStateException("Could not find root type in specification files.")

            val element = JsonSerializer.deserialize(
                json, context, type
            )

            if (element !is MemoryObject) throw IllegalStateException("Can only input object.")
            element
        } else null))

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

    val output = command.output
    val evaluation = program.evaluate()
    if (evaluation == null) {
        println("UnknownError: Evaluation returned null")
    } else if (output != null) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        output.writeText(gson.toJson(JsonSerializer.serialize(evaluation, program.evaluate)))
        println("Result was written in $output.")
    } else {
        println(evaluation)
    }
}