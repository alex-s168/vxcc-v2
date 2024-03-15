package vxcc.asm.ua16

import vxcc.asm.Assembler
import vxcc.asm.assemble
import vxcc.asm.parseNum

class UA16Assembler(
    val origin: Int
): Assembler {
    val labels = mutableMapOf<String, Int>()
    private var next = origin
    private var bytes = ByteArray(0)
    private val refs = mutableListOf<Triple<Int, String, String>>()

    fun finish(): ByteArray {
        refs.forEach { ref ->
            next = ref.first
            var value = labels[ref.second]!!
            val dest = ref.third
            repeat(4) {
                assemble("sbr ${value and 0xF}", this)
                assemble("stb $dest", this)
                value = value shr 4
            }
        }
        refs.clear()
        return bytes
    }

    private fun data(data: ByteArray) {
        if (next - origin + data.size > bytes.size)
            bytes = bytes.copyOf(next - origin + data.size)
        data.copyInto(bytes, next - origin)
        next += data.size
    }

    private fun byteBits(bitsIn: String, vararg replIn: Pair<String, Int>) {
        val repl = mapOf(*replIn)
        var bits = bitsIn.replace('.', '0')
        repl.forEach { (k, v) ->
            bits = bits.replace(k, v.toString(2))
        }
        val byte = bits.toUByte(2).toByte()
        data(byteArrayOf(byte))
    }

    private fun parseReg(name: String, selectPc: Boolean = false): Int =
        when (name) {
            "r0" -> 0
            "r1" -> 1
            "r2" -> 2
            "c1" -> throw Exception("'c1' should not be used in assembly! Use '1' instead!")
            "1" -> if (!selectPc) 3 else throw Exception("Can not use constant 1 for this instruction; program counter register selected instead!")
            "pc" -> if (selectPc) 3 else throw Exception("Can nto use program counter register for this instruction; constant 1 is selected instead!")
            else -> throw Exception("Unknown register $name!")
        }

    private fun parseLabelOrNum(name: String): Int =
        labels[name] ?: parseNum(name)

    override fun label(name: String, flags: Map<String, String?>) {
        if ("orig" in flags) {
            next = parseNum(flags["orig"]!!)
        }
        labels[name] = next
    }

    override fun instruction(name: String, args: List<String>, flags: Map<String, String?>) {
        if ("orig" in flags) {
            next = parseNum(flags["orig"]!!)
        }
        when (name) {
            "adc" -> byteBits("0000ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            "sbc" -> byteBits("0001ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            "ec9" -> byteBits("0010..ss", "ss" to parseReg(args[0]))
            "fwc" -> byteBits("0011..dd", "dd" to parseReg(args[0]))
            "tst" -> byteBits("0100..ss", "ss" to parseReg(args[0]))
            "ltu" -> byteBits("0101aabb", "aa" to parseReg(args[0]), "bb" to parseReg(args[1]))
            "orr" -> byteBits("0110ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            "clc" -> byteBits("0111...0")
            "inv" -> byteBits("0111...1")
            "stb" -> byteBits("1000.0dd", "dd" to parseReg(args[0]))
            "ldb" -> byteBits("1000.1ss", "ss" to parseReg(args[0]))
            "bnc" -> byteBits("1001..rr", "rr" to parseReg(args[0]))
            "sbr" -> byteBits("1010vvvv", "vvvv" to parseNum(args[0]))
            "phr" -> byteBits("1011.0rr", "rr" to parseReg(args[0]))
            "plr" -> byteBits("1011.1rr", "rr" to parseReg(args[0]))
            "@ext" -> byteBits("1100eeee", "eeee" to parseNum(args[0]))
            "mov" -> byteBits("1101ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            "lod" -> byteBits("1110bbaa", "bb" to parseReg(args[0]), "aa" to parseReg(args[1]))
            "sto" -> byteBits("1111bbaa", "bb" to parseReg(args[0]), "aa" to parseReg(args[1]))
            "@imm" -> {
                val dest = args[0]
                try {
                    var value = parseLabelOrNum(args[1])
                    repeat(4) {
                        assemble("sbr ${value and 0xF}", this)
                        assemble("stb $dest", this)
                        value = value shr 4
                    }
                } catch (ignored: Exception) {
                    refs += Triple(next, args[1], dest)
                    next += 8
                }
            }
            "@callnc" -> {
                val addrReg = args[0]
                assemble("phr pc", this)
                assemble("bnc $addrReg", this)
            }
            "@retnc" -> {
                val clob = args[0].split("clob=").getOrNull(1) ?: throw Exception("Invalid usage! Usage: @retnc clob=reg")
                assemble("plr $clob", this)
                assemble("clc", this)
                assemble("adc $clob, 1", this)
                assemble("adc $clob, 1", this)
                assemble("bnc $clob", this)
            }
            else -> throw Exception("Unknown instruction or macro $name!")
        }
    }

    override fun data(bytes: ByteArray, flags: Map<String, String?>) {
        if ("orig" in flags) {
            next = parseNum(flags["orig"]!!)
        }
        data(bytes)
    }
}