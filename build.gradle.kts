plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.gitlab.arturbosch.detekt") apply true
    id("com.diffplug.gradle.spotless") apply true
    id("maven-publish")
    id("org.owasp.dependencycheck") apply true
}

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

task("checkJavaVersion") {
    if (!JavaVersion.current().isJava8) {
        throw IllegalStateException(
                "ERROR: Java 1.8 required but " + JavaVersion.current() + " found. Change your JAVA_HOME environment variable."
        )
    }
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
        freeCompilerArgs = listOf("-Xjvm-default=compatibility")
    }
}

tasks.withType<Jar> {
    // This makes the JAR's SHA-256 hash repeatable.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
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
