package com.ing.zknotary.common.zkp

import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
@Suppress("DEPRECATION")
class ZincZKService(
    circuitFolder: String,
    artifactFolder: String,
    private val buildTimeout: Duration,
    private val setupTimeout: Duration,
    private val provingTimeout: Duration,
    private val verificationTimeout: Duration
) {

    private val circuitManifestPath = "$circuitFolder/Zargo.toml"
    private val defaultBuildPath = "$circuitFolder/build"
    private val defaultDataPath = "$circuitFolder/data"

    val compiledCircuitPath = "$artifactFolder/compiled-circuit.znb"
    val zkSetup = ZKSetup(
        provingKeyPath = "$artifactFolder/proving_key",
        verifyingKeyPath = "$artifactFolder/verifying_key.txt"
    )

    companion object {
        const val BUILD = "zargo build"
        const val SETUP = "zargo setup"
        const val VERIFY = "zargo verify"
        const val RUN = "zargo run"

        /**
         * Returns output of the command execution.
         **/
        fun completeZincCommand(command: String, timeout: Duration, input: File? = null): String {
            val process = command.toProcess(input)
            val hasCompleted = process.waitFor(timeout.seconds, TimeUnit.SECONDS)

            if (!hasCompleted) {
                process.destroy()
                error("$command ran longer than ${timeout.seconds} seconds")
            }

            return process.errorStream.bufferedReader().readText() + process.inputStream.bufferedReader().readText()
        }

        private fun String.toProcess(input: File? = null): Process {
            return try {
                val builder = ProcessBuilder(split("\\s".toRegex()))
                if (input != null) {
                    builder.redirectInput(input)
                }
                builder.start()
            } catch (e: IOException) {
                error(e.localizedMessage)
            }
        }
    }

    data class ZKSetup(val provingKeyPath: String, val verifyingKeyPath: String)

    fun cleanup() {
        listOf(
            compiledCircuitPath,
            zkSetup.provingKeyPath,
            zkSetup.verifyingKeyPath
        ).forEach { File(it).delete() }
    }

    fun setup() {
        val circuitManifest = File(circuitManifestPath)
        require(circuitManifest.exists()) { "Cannot find circuit manifest at $circuitManifestPath" }

        val witnessFile = createTempFile()
        val publicData = createTempFile()

        try {
            completeZincCommand(
                "$BUILD --manifest-path $circuitManifestPath --circuit $compiledCircuitPath " +
                        "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
                buildTimeout
            )
        } finally {
            // Neither witness, nor Public data carry useful information after build, they are just templates
            publicData.delete()
            witnessFile.delete()
            // Zinc creates files in the default locations independently if it was specified the exact locations,
            // clear the defaults too.
            File(defaultBuildPath).deleteRecursively()
            File(defaultDataPath).deleteRecursively()
        }
        require(File(compiledCircuitPath).exists()) { "Compile circuit not found in path $compiledCircuitPath." }

        completeZincCommand(
            "$SETUP --circuit $compiledCircuitPath " +
                    "--proving-key ${zkSetup.provingKeyPath} --verifying-key ${zkSetup.verifyingKeyPath}",
            setupTimeout
        )
        require(File(zkSetup.provingKeyPath).exists()) { "Proving key not found at ${zkSetup.provingKeyPath}." }
    }

    fun run(serialized: ByteArray): String {

        val witness = "{\"input\": [${serialized.map { "\"${it.toUByte()}\"" }.joinToString(",")}] }".toByteArray()

        val witnessFile = createTempFile()
        witnessFile.writeBytes(witness)

        val publicData = createTempFile()

        try {
            val s =  completeZincCommand(
                "$RUN --circuit $compiledCircuitPath --manifest-path $circuitManifestPath " +
                        "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
                provingTimeout
            )
            return s
        } catch (e: Exception) {
            throw RuntimeException("Could not create proof. Cause: $e\n")
        } finally {
            publicData.delete()
        }
    }

    fun verify(proof: ByteArray, serialized: ByteArray) {

        val publicInput = "{\"input\": [${serialized.joinToString(",")}] }".toByteArray()

        val proofFile = createTempFile()
        proofFile.writeBytes(proof)

        val publicDataFile = createTempFile()
        publicDataFile.writeBytes(publicInput)

        try {
            completeZincCommand(
                "$VERIFY --circuit $compiledCircuitPath --verifying-key ${zkSetup.verifyingKeyPath} --public-data ${publicDataFile.absolutePath}",
                verificationTimeout, proofFile
            )
        } catch (e: Exception) {
            throw RuntimeException(
                "Could not verify proof.\nCause: $e"
            )
        } finally {
            proofFile.delete()
            publicDataFile.delete()
        }
    }
}