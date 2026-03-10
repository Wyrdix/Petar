package fr.univ_lille.iut_info.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.converters.FileConverter
import java.io.File

class Command {

    @Parameter(names = ["--help"], help = true)
    var help: Boolean = false

    @Parameter(names = ["-p"], description = "Print specification")
    var printSpecification: Boolean = false;

    @Parameter(names = ["--spec", "-s"], description = "Provide specification files", converter = FileConverter::class, required = true)
    var specifications: List<File> = ArrayList()
}