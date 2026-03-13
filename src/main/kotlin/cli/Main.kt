package fr.univ_lille.iut_info.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.google.gson.JsonParser
import fr.univ_lille.iut_info.NodeDeclarationStatement
import fr.univ_lille.iut_info.evaluation.evaluate
import fr.univ_lille.iut_info.name.NameAnalysis
import fr.univ_lille.iut_info.parsing.Parser
import fr.univ_lille.iut_info.parsing.createMemoryElement
import fr.univ_lille.iut_info.type.TypeCheck
import fr.univ_lille.iut_info.type.safeCheck

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


    if (command.inputs.find { it.extension != "json" } != null) {
        println("Error in input file list, only json files are accepted.")
        return
    }

    if (command.inputs.find { !it.exists() } != null) {
        println("The following input files do not exist :")
        command.inputs.filterNot { it.exists() }.forEach { println("\t-${it.absolutePath}") }
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

    if (command.inputs.isNotEmpty()) {

        val availableRoots: List<NodeDeclarationStatement> =
            nameAnalysis.names.values.filterIsInstance<NodeDeclarationStatement>()
                .filter { it.identifier.lowercase() == "root" || it.type.interfaces.find { group -> group.lowercase() == "root" } != null }

        if (availableRoots.isEmpty()) {
            println("NameError: No Root node type could be found. Either defined a Node named 'Root', or a 'Root' group and add this group to the node you wish to represent the whole document (they can be multiple ones).")
            return
        }


        val nullableElements = command.inputs.map { file ->
            println("Parsing input file : $file.")

            val jsonObject = JsonParser.parseString(file.readLines().joinToString(separator = "")).asJsonObject
            val suitableRoots = availableRoots.filter { it.type.safeCheck(jsonObject) }

            if (suitableRoots.isEmpty()) {
                println(
                    "No suitable root were found (available roots are [${
                        availableRoots.joinToString(separator = ",") { it.identifier }
                    }])"
                )
                return@map Pair(file, null)
            } else if (suitableRoots.size > 1) {
                println(
                    "Multiple suitable roots were found (suitable roots are [${
                        suitableRoots.joinToString(separator = ",") { it.identifier }
                    }])"
                )
                return@map Pair(file, null)
            }

            val element = createMemoryElement(suitableRoots[0].type, jsonObject)
            return@map Pair(file, element)
        }

        if(nullableElements.find { it.second == null } != null) return

        val elements = nullableElements.map { (file, element) -> Pair(file, element!!) }

        elements.forEach { (file, element) ->
            println("Transforming file $file :")
            val evaluation = statements.evaluate(element)
            println(evaluation)
        }
    }
}