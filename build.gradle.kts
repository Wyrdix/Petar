plugins {
    application
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.4.1"
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
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.generateGrammarSource {
    outputDirectory = file("${project.projectDir}/src/main/java/fr/univ_lille/iut_info/petar_parser")

    arguments = listOf("-Dlanguage=Java", "-package", "fr.univ_lille.iut_info.petar_parser")
}


tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}

tasks.compileTestKotlin {
    dependsOn("generateTestGrammarSource")
}

application {
    mainClass = "fr.univ_lille.iut_info.cli.MainKt"
}