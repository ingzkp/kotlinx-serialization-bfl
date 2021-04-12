plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    jacoco
    `java-library`
    id("io.gitlab.arturbosch.detekt") apply true
    id("com.diffplug.spotless") apply true
    id("maven-publish")
    id("org.owasp.dependencycheck") apply true
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("reflect"))

    val kotlinSerializationVersion: String by project
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinSerializationVersion")

    testImplementation(kotlin("test-junit5"))

    val junit5Version: String by project
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")

    val kotlinTestVersion: String by project
    testImplementation("io.kotest:kotest-assertions-core:$kotlinTestVersion")
}

tasks.test {
    useJUnitPlatform()
}

/**
 * Reified serialization and deserialization functions are currently excluded from the report, since Jacoco does not
 * support coverage for inline functions. These functions are actually tested, yet their inclusion in the report would
 * result to zero coverage on their side, thus a falsified view of the actual test coverage of the project.
 */
tasks.withType<JacocoReport> {
    dependsOn(tasks.test)

    // TODO: When Jacoco adds support for inline functions, remove this exclusion
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it).apply {
                exclude("com/ing/serialization/bfl/api/reified/**")
            }
        }))
    }
}

task("checkJavaVersion") {
    if (!JavaVersion.current().isJava8) {
        error(
            "ERROR: Java 1.8 required but " + JavaVersion.current() + " found. Change your JAVA_HOME environment variable."
        )
    }
}

dependencyCheck {
    suppressionFile = projectDir.resolve("config/owasp/suppressions.xml").absolutePath
    analyzers.apply {
        assemblyEnabled = false
        nodeEnabled = false
        retirejs.enabled = false
    }
    failBuildOnCVSS = 6.9F
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(":checkJavaVersion")
    dependsOn("spotlessApply") // Autofix before check
    dependsOn("spotlessCheck") // Fail on remaining non-autofixable issues

    kotlinOptions {
        languageVersion = "1.4"
        apiVersion = "1.4"
        jvmTarget = "1.8"
        javaParameters = true // Useful for reflection.
        freeCompilerArgs =
                listOf(
                        "-Xjvm-default=compatibility",
                        "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
                )
    }
}

java {
    withSourcesJar()
}

tasks.withType<Jar> {
    // This makes the JAR's SHA-256 hash repeatable.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    manifest {
        attributes(mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
        ))
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    jvmTarget = "1.8"
    config.setFrom("$rootDir/config/detekt/detekt.yml")

    parallel = true

    source(files(rootProject.projectDir))
    include("**/*.kt")
    exclude("**/*.kts")
    exclude("**/resources/")
    exclude("**/build/")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("**/*.kt")
        targetExclude("${buildDir.relativeTo(rootDir).path}/generated/**")
        val ktlintVersion: String by project
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts") // default target for kotlinGradle
        ktlint() // or ktfmt() or prettier()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ingzkp/kotlinx-serialization-bfl")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
