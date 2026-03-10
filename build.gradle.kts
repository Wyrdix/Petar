plugins {
    kotlin("jvm") version "2.3.0"
}

group = "fr.univ_lille.iut_info"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}