import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.gitlab.arturbosch.detekt") version "1.16.0"
}

group = "me.vic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    testImplementation(kotlin("test-junit5"))

    val junit5Version: String by project
    val kotlinTestVersion: String by project

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testImplementation("io.kotest:kotest-assertions-core:$kotlinTestVersion")

    val kotlinSerializationVersion: String by project
    val cordaVersion: String by project
    val quasarVersion: String by project
    //
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinSerializationVersion")
    implementation("net.corda:corda-core:$cordaVersion")
    implementation("co.paralleluniverse:quasar-core:$quasarVersion")

    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    jvmTarget = "1.8"
    config.setFrom("${rootDir}/config/detekt/detekt.yml")

    parallel = true

    source(files(rootProject.projectDir))
    include("**/*.kt")
    exclude("**/*.kts")
    exclude("**/resources/")
    exclude("**/build/")
}
