package fr.univ_lille.iut_info.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import fr.univ_lille.iut_info.NodeDeclarationStatement
import fr.univ_lille.iut_info.evaluation.evaluate
import fr.univ_lille.iut_info.name.NameAnalysis
import fr.univ_lille.iut_info.parsing.MemoryElement
import fr.univ_lille.iut_info.parsing.Parser
import fr.univ_lille.iut_info.parsing.createMemoryElement
import fr.univ_lille.iut_info.type.TypeCheck
import fr.univ_lille.iut_info.type.safeCheck
import java.io.FileWriter

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

    val nameAnalysis = NameAnalysis(statements)
    val nameErrors = nameAnalysis.check()

    if (nameErrors.isNotEmpty()) {
        nameErrors.forEach { println(it) }
        return
    }
    nameAnalysis.resolveReference()

    val typeAnalysis = TypeCheck(nameAnalysis)
    val typeErrors = typeAnalysis.check()

    if (typeErrors.isNotEmpty()) {
        typeErrors.forEach { println(it) }
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

    if (input != null) {

        val availableRoots: List<NodeDeclarationStatement> =
            nameAnalysis.names.values.filterIsInstance<NodeDeclarationStatement>()
                .filter { it.identifier.lowercase() == "root" || it.type.interfaces.find { group -> group.lowercase() == "root" } != null }

        if (availableRoots.isEmpty()) {
            println("NameError: No Root node type could be found. Either defined a Node named 'Root', or a 'Root' group and add this group to the node you wish to represent the whole document (they can be multiple ones).")
            return
        }

        println("Parsing input file : $input.")

        val jsonObject = JsonParser.parseString(input.readLines().joinToString(separator = "")).asJsonObject
        val suitableRoots = availableRoots.filter { it.type.safeCheck(jsonObject) }

        var result: MemoryElement? = null
        if (suitableRoots.isEmpty()) {
            println(
                "No suitable root were found (available roots are [${
                    availableRoots.joinToString(separator = ",") { it.identifier }
                }])"
            )
        } else if (suitableRoots.size > 1) {
            println(
                "Multiple suitable roots were found (suitable roots are [${
                    suitableRoots.joinToString(separator = ",") { it.identifier }
                }])"
            )
        } else {
            val element = createMemoryElement(suitableRoots[0].type, jsonObject)
            result = element
        }

        if (result == null) return

        println("Transforming file $input :")
        val evaluation = statements.evaluate(result)

        val output = command.output
        if(output != null){
            val gson = GsonBuilder().setPrettyPrinting().create()
            output.writeText(gson.toJson(evaluation.toJson()))
            println("Result was written in $output.")
        } else {
            println(evaluation)
        }
    }
}