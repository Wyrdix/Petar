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
    fun testsFromFiles(): Stream<DynamicTest> {
        val file = File("src/test/test_files")
        return file.walk().filter { parent -> parent.isDirectory() && parent.listFiles().none { it.isDirectory } }
            .flatMap { directory ->

                val specifications = directory.listFiles().filter { it.extension == "petar" }

                val subTests = directory.listFiles()
                    .filter { it.nameWithoutExtension.startsWith("input") && it.extension == "json" }

                subTests
                    .map { input ->
                        val radical = input.nameWithoutExtension.substring("input".length)
                        val expected =
                            File(directory, "output$radical.json")

                        val actual = File(expected.absolutePath + ".actual")
                        val testName =
                            if (subTests.size == 1) directory.toRelativeString(file)
                            else directory.toRelativeString(file) + "/${radical}"

                        DynamicTest.dynamicTest(
                            testName
                        ) {
                            try {
                                main(
                                    specifications.flatMap { listOf("-s", it.absolutePath) }
                                        .toTypedArray() + arrayOf(
                                        "-r", "Root"
                                    ) + arrayOf("-i", input.absolutePath) + arrayOf("-o", actual.absolutePath)
                                )

                                assertEquals(
                                    Files.readString(expected.toPath()),
                                    Files.readString(actual.toPath())
                                )
                            } catch (e: Exception) {
                                throw e
                            } finally {
                                if (actual.exists()) actual.delete()
                            }

                        }
                    }

            }.asStream()
    }
}
