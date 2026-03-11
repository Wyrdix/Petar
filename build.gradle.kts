plugins {
    kotlin("jvm") version "2.3.0"
    antlr
}

group = "fr.univ_lille.iut_info"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.5")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.beust:jcommander:1.82")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}

tasks.generateGrammarSource {
    outputDirectory = file("${project.projectDir}/src/main/java/fr/univ_lille/iut_info/alfr_parser")

    arguments = listOf("-Dlanguage=Java", "-package", "fr.univ_lille.iut_info.alfr_parser")
}


tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}

tasks.compileTestKotlin {
    dependsOn("generateTestGrammarSource")
}