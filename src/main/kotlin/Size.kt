import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.reflect.KClass

object Size {
    private val sizes = mutableMapOf<KClass<*>, Int>()

    fun of(name: String, defaults: List<out Any>): Int {
        val clazz = Class.forName(name)

        val default = defaults.firstOrNull {
            it::class.java.canonicalName == clazz.canonicalName
        } ?: error("No default for ${clazz.canonicalName} has been provided")

        return when (default) {
            // TODO Must be generated
            //  this is done by collecting all @Serializable classes.
            is DateSurrogate -> memoizeAndGet(default)
            is Own -> memoizeAndGet(default)
            // <-
            else -> error("${default::class.simpleName} must be serializable")
        }
    }

    private inline fun <reified T: Any> memoizeAndGet(arg: T) =
        sizes.computeIfAbsent(arg::class) {
            val output = ByteArrayOutputStream()
            encodeTo(DataOutputStream(output), arg)
            output.size()
        }
}