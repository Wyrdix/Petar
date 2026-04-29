import fr.univ_lille.iut_info.cli.main
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.assertEquals

class TestFiles {
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @TestFactory
    fun dynamicTestsFromFiles(): Stream<DynamicTest> {
        val file = File("src/test/test_files")
        println("${file.exists()} ${file.path}")
        return file.walk().filter { parent -> parent.isDirectory() && parent.listFiles().none { it.isDirectory } }
            .map { directory ->

                val specifications = directory.listFiles().filter { it.extension == "alfr" }
                val input = File(directory, "input.json")
                val expected = File(directory, "output.json")
                val actual = File(directory, "output.json.actual")

                if (actual.exists()) actual.delete()

                val testName = directory.toRelativeString(file)
                return@map DynamicTest.dynamicTest(
                    testName
                ) {
                    try {
                        main(
                            specifications.flatMap { listOf("-s", it.absolutePath) }.toTypedArray() + arrayOf(
                                "-r",
                                "Root"
                            ) + arrayOf("-i", input.absolutePath) + arrayOf("-o", actual.absolutePath)
                        )

                        assertEquals(Files.readString(expected.toPath()), Files.readString(actual.toPath()))
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        if (actual.exists()) actual.delete()
                    }

                }
            }.asStream()
    }
}
