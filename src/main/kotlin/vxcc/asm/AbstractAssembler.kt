package vxcc.asm

import blitz.term.Terminal
import vxcc.asm.etca.ETCAAssembler
import vxcc.arch.AbstractTarget
import kotlin.math.abs

// TODO: local labels (starting with '.')
abstract class AbstractAssembler<T: AbstractAssembler<T>>(
    val origin: Int,
    val target: AbstractTarget,
    val instructions: Map<String, Instruction<T>>
): Assembler {
    data class Instruction<T: AbstractAssembler<T>>(
        val requiresExt: List<String> = listOf(),
        val parse: T.(List<String>) -> Unit
    )

    data class LabelData(
        val pos: Int,
        val export: Boolean,
    )

    val labels = mutableMapOf<String, LabelData>()
    protected var next = origin
    protected var bytes = ByteArray(0)

    protected fun data(data: ByteArray) {
        if (next - origin + data.size > bytes.size)
            bytes = bytes.copyOf(next - origin + data.size)
        data.copyInto(bytes, next - origin)
        next += data.size
    }

    protected fun byteBits(bitsIn: String, vararg replIn: Pair<String, Int>) {
        if (bitsIn.length != 8)
            error("bitsIn not 8 bits!")
        val repl = mapOf(*replIn)
        var bits = bitsIn.replace('.', '0')
        repl.forEach { (k, v) ->
            val neg = v < 0
            var e = abs(v).toString(2).padStart(k.length, '0')
            if (e.length != k.length)
                error("Too big value")
            if (neg) {
                val t = e.toUInt(2).inv() + 1u
                e = t.toUByte().toString(2).padStart(k.length, '0')
            }
            bits = bits.replace(k, e)
        }
        val byte = bits.toUByte(2).toByte()
        data(byteArrayOf(byte))
    }

    override fun data(bytes: ByteArray, flags: Map<String, String?>) {
        if ("orig" in flags)
            next = parseNum(flags["orig"]!!)

        if (!flags.keys.all { it == "orig" })
            error("Unexpected flag! Allowed here: orig")

        data(bytes)
    }

    override fun label(name: String, flags: Map<String, String?>) {
        if ("orig" in flags)
            next = parseNum(flags["orig"]!!)

        val export = "export" in flags

        val all = listOf("orig", "export")
        if (!flags.keys.all { it in all })
            error("Unexpected flag! Allowed here: orig")

        labels[name] = LabelData(next, export)
    }

    override fun instruction(name: String, args: List<String>, flags: Map<String, String?>) {
        try {
            if ("orig" in flags)
                next = parseNum(flags["orig"]!!)

            if (!flags.keys.all { it == "orig" })
                error("Unexpected flag! Allowed here: orig")

            val inst = instructions[name] ?: error("Instruction $name not found!")

            inst.requiresExt.firstOrNull { it !in target.targetFlags }?.let {
                error("Required target flag $it for instruction $inst not set!")
            }

            inst.parse(this as T, args)
        } catch (e: Exception) {
            error("Error in instruction \"$name ${args.joinToString()}\": ${e.message}")
        }
    }

    protected fun asm(str: String) =
        assemble(str, this)

    protected fun parseLabelOrNum(name: String): Int =
        labels[name]?.pos ?: parseNum(name)

    fun <R> requireFeat(name: String, r: R): R {
        if (name !in target.targetFlags)
            error("Required feature $name not present!")
        return r
    }

    fun requireFeat(name: String) =
        requireFeat(name, Unit)

    companion object {
        @JvmStatic
        protected fun depInstrName(old: String, new: String): Instruction<ETCAAssembler> =
            Instruction { args ->
                Terminal.errln("Deprecated instruction name \"$old\". Use \"$new\" instead.")
                instruction(new, args, mapOf())
            }

        @JvmStatic
        protected fun alias(to: String): Instruction<ETCAAssembler> =
            Instruction { instruction(to, it, mapOf()) }
    }
}
