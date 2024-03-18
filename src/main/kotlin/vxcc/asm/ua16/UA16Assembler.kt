package vxcc.asm.ua16

import vxcc.arch.ua16.UA16Target
import vxcc.asm.AbstractAssembler
import vxcc.asm.assemble
import vxcc.asm.parseNum

class UA16Assembler(
    origin: Int,
    target: UA16Target
): AbstractAssembler<UA16Assembler>(origin, target, instructions) {
    companion object {
        private val instructions = mapOf<String, Instruction<UA16Assembler>>(
            "adc" to Instruction { args ->
                byteBits("0000ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            },
            "not" to Instruction { args ->
                byteBits("0001ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            },
            "ec9" to Instruction { args ->
                byteBits("0010..ss", "ss" to parseReg(args[0]))
             },
            "fwc" to Instruction { args ->
                byteBits("0011..dd", "dd" to parseReg(args[0]))
            },
            "tst" to Instruction { args ->
                byteBits("0100..ss", "ss" to parseReg(args[0]))
            },
            "ltu" to Instruction { args ->
                byteBits("0101aabb", "aa" to parseReg(args[0]), "bb" to parseReg(args[1]))
            },
            "orr" to Instruction { args ->
                byteBits("0110ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            },
            "clc" to Instruction { _ ->
                byteBits("0111...0")
            },
            "inv" to Instruction { _ ->
                byteBits("0111...1")
            },
            "stb" to Instruction { args ->
                byteBits("1000.0dd", "dd" to parseReg(args[0]))
            },
            "ldb" to Instruction { args ->
                byteBits("1000.1ss", "ss" to parseReg(args[0]))
            },
            "bnc" to Instruction { args ->
                byteBits("1001..rr", "rr" to parseReg(args[0]))
            },
            "sbr" to Instruction { args ->
                byteBits("1010vvvv", "vvvv" to parseNum(args[0]))
            },
            "phr" to Instruction { args ->
                byteBits("1011.0rr", "rr" to parseReg(args[0]))
            },
            "plr" to Instruction { args ->
                byteBits("1011.1rr", "rr" to parseReg(args[0]))
            },
            "@ext" to Instruction { args ->
                byteBits("1100eeee", "eeee" to parseNum(args[0]))
            },
            "mov" to Instruction { args ->
                byteBits("1101ddss", "dd" to parseReg(args[0]), "ss" to parseReg(args[1]))
            },
            "lod" to Instruction { args ->
                byteBits("1110bbaa", "bb" to parseReg(args[0]), "aa" to parseReg(args[1]))
             },
            "sto" to Instruction { args ->
                byteBits("1111bbaa", "bb" to parseReg(args[0]), "aa" to parseReg(args[1]))
             },
            "@imm" to Instruction { args ->
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
            },
            "@callnc" to Instruction { args ->
                val addrReg = args[0]
                assemble("phr pc", this)
                assemble("bnc $addrReg", this)
            },
            "@retnc" to Instruction { args ->
                if (!args[0].startsWith("clob="))
                    throw Exception("Invalid usage! Usage: @retnc clob=reg")
                val clob = args[0].substringAfter("clob=")
                assemble("plr $clob", this)
                assemble("clc", this)
                assemble("adc $clob, 1", this)
                assemble("adc $clob, 1", this)
                assemble("bnc $clob", this)
            },
            "@sbc" to Instruction { args ->
                val dest = args[0]
                val src = args[1]
                if (!args[2].startsWith("clob="))
                    throw Exception("Invalid usage! Usage: @sbc dest, src, clob=reg")
                val clob = args[2].substringAfter("clob=")

                assemble("not $clob, $src", this)
                assemble("adc $clob, 1", this)
                assemble("adc $dest, $clob", this)
            }
        )
    }

    private val refs = mutableListOf<Triple<Int, String, String>>()

    override fun finish(): ByteArray {
        refs.forEach { ref ->
            next = ref.first
            var value = labels[ref.second]!!.pos
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
}