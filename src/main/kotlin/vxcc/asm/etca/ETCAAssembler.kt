package vxcc.asm.etca

import vxcc.arch.etca.ETCATarget
import vxcc.asm.AbstractAssembler
import vxcc.asm.parseNum

class ETCAAssembler(
    origin: Int,
    target: ETCATarget
): AbstractAssembler<ETCAAssembler>(origin, target, instructions) {
    companion object {

        private val instructions = mapOf<String, Instruction<ETCAAssembler>>(
            "add"   to Instruction { simpleOp(0b0000, it) },
            "sub"   to Instruction { simpleOp(0b0001, it) },
            "rsub"  to Instruction { simpleOp(0b0010, it) },
            "cmp"   to Instruction { simpleOp(0b0011, it) },
            "or"    to Instruction { simpleOp(0b0100, it) },
            "xor"   to Instruction { simpleOp(0b0101, it) },
            "and"   to Instruction { simpleOp(0b0110, it) },
            "test"  to Instruction { simpleOp(0b0111, it) },
            "movz"  to Instruction { simpleOp(0b1000, it) },
            "movs"  to Instruction { simpleOp(0b1001, it) },
            "load"  to Instruction { simpleOp(0b1010, it) },
            "store" to Instruction { simpleOp(0b1011, it) },
            "slo"   to Instruction { regImmOp(0b1100, it) },
            // res
            "readcr"  to Instruction { regImmOp(0b1110, it) },
            "writecr" to Instruction { regImmOp(0b1111, it) },
        )
    }

    fun parseReg(reg: String): Int {
        if (reg.startsWith('r')) {
            val id = reg.substring(1).toUIntOrNull()
                ?: throw Exception("Invalid register!")

            if (id > 7u)
                throw Exception("Invalid register!")

            return id.toInt()
        }

        throw Exception("Invalid register!")
    }

    fun simpleOp(opcode: Int, args: List<String>) {
        val a = parseReg(args[0])
        val b = kotlin.runCatching { parseReg(args[1]) }.getOrNull()
        if (b != null) { // reg reg
            byteBits("0001cccc", "cccc" to opcode)
            byteBits("dddsss00", "ddd" to a, "sss" to b)
        } else { // reg imm
            val imm = parseNum(args[1])
            byteBits("0101cccc", "cccc" to opcode)
            byteBits("dddiiiii", "ddd" to a, "iiiii" to imm)
        }
    }

    fun regImmOp(opcode: Int, args: List<String>) {
        val a = parseReg(args[0])
        val imm = parseNum(args[1])
        byteBits("0101cccc", "cccc" to opcode)
        byteBits("dddiiiii", "ddd" to a, "iiiii" to imm)
    }

    override fun finish(): ByteArray {
        return bytes
    }
}