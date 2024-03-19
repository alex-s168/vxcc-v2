package vxcc.asm.etca

import vxcc.arch.etca.ETCATarget
import vxcc.asm.AbstractAssembler
import vxcc.asm.parseNum

class ETCAAssembler(
    origin: Int,
    target: ETCATarget
): AbstractAssembler<ETCAAssembler>(origin, target, instructions) {
    companion object {

        private val instructions = mapOf(
            "add" to Instruction { simpleOp(0b0000, it) },
            "sub" to Instruction { simpleOp(0b0001, it) },
            "rsub" to Instruction { simpleOp(0b0010, it) },
            "cmp" to Instruction { simpleOp(0b0011, it) },
            "or" to Instruction { simpleOp(0b0100, it) },
            "xor" to Instruction { simpleOp(0b0101, it) },
            "and" to Instruction { simpleOp(0b0110, it) },
            "test" to Instruction { simpleOp(0b0111, it) },
            "movz" to Instruction { simpleOp(0b1000, it) },
            "movs" to Instruction {
                if (it[0].startsWith('[')) {
                    // store
                    simpleOp(0b1011, listOf(it[0].drop(1).dropLast(1), it[1]))
                } else if (it[1].startsWith('[')) {
                    // load
                    simpleOp(0b1010, listOf(it[0], it[1].drop(1).dropLast(1)))
                } else {
                    try {
                        simpleOp(0b1001, it)
                    } catch (ignored: Exception) {
                        try {
                            try {
                                // readcr
                                val dest = parseReg(it[0])
                                val src = parseCR(it[1])
                                byteBits("01011110")
                                byteBits("dddiiiii", "ddd" to dest, "iiiii" to src)
                            } catch (ignored: Exception) {
                                // writecr
                                val dest = parseCR(it[0])
                                val src = parseReg(it[1])
                                byteBits("01011111")
                                byteBits("dddiiiii", "ddd" to src, "iiiii" to dest)
                            }
                        } catch (ignored: Exception) {
                            throw Exception("Invalid combination of operands")
                        }
                    }
                }
            },
            "mov" to alias("movs"),
            "load" to depInstrName("load", "mov"),
            "store" to depInstrName("store", "mov"),
            "slo" to Instruction { regImmOp(0b1100, it) },
            // reserved
            "readcr" to depInstrName("readcr", "mov"),
            "writecr" to depInstrName("writecr", "mov"),

            "jmp.z" to Instruction { jumpOp(0b0000, it) },
            "jmp.e" to Instruction { jumpOp(0b0000, it) },
            "jmp.nz" to Instruction { jumpOp(0b0001, it) },
            "jmp.ne" to Instruction { jumpOp(0b0001, it) },
            "jmp.n" to Instruction { jumpOp(0b0010, it) },
            "jmp.nn" to depInstrName("jmp.nn", "jmp.p"),
            "jmp.p" to Instruction { jumpOp(0b0011, it) },
            "jmp.c" to Instruction { jumpOp(0b0100, it) },
            "jmp.b" to Instruction { jumpOp(0b0100, it) },
            "jmp.nc" to Instruction { jumpOp(0b0101, it) },
            "jmp.ae" to Instruction { jumpOp(0b0101, it) },
            "jmp.o" to Instruction { jumpOp(0b0110, it) },
            "jmp.no" to Instruction { jumpOp(0b0111, it) },
            "jmp.be" to Instruction { jumpOp(0b1000, it) },
            "jmp.a" to Instruction { jumpOp(0b1001, it) },
            "jmp.l" to Instruction { jumpOp(0b1010, it) },
            "jmp.ge" to Instruction { jumpOp(0b1011, it) },
            "jmp.le" to Instruction { jumpOp(0b1100, it) },
            "jmp.g" to Instruction { jumpOp(0b1101, it) },
            "jmp" to Instruction { jumpOp(0b1110, it) },

            "push" to Instruction(listOf("stack")) { // TODO: immediate mode
                byteBits("00011101")
                byteBits("...bbb00", "bbb" to parseReg(it[0]))
            },
            "pop" to Instruction(listOf("stack")) {
                byteBits("00011100")
                byteBits("aaa...00", "aaa" to parseReg(it[0]))
            },
        )
    }

    fun parseCR(reg: String): Int =
        when (reg) {
            "cpuid1" -> 0
            "cpuid2" -> 1
            "feat" -> 2
            else -> throw Exception("Unknown control register $reg")
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

    private data class JmpRelRef(
        val where: Int,
        val opcode: Int,
        val dest: String,
    )

    private val refs = mutableListOf<JmpRelRef>()

    fun jumpOp(opcode: Int, args: List<String>) {
        refs += JmpRelRef(next, opcode, args[0])
        next += 2
    }

    override fun finish(): ByteArray {
        for (ref in refs) { // relative
            next = ref.where
            val dest = parseLabelOrNum(ref.dest) - ref.where
            if (dest > Byte.MAX_VALUE * 2)
                throw Exception("jump rel9 too big")
            if (dest < Byte.MIN_VALUE * 2)
                throw Exception("jump rel9 too small")

            val destBin = dest.toString(2).padStart(9, '0')

            byteBits("100dcccc", "d" to "${destBin[0]}".toInt(2), "cccc" to ref.opcode)
            data(byteArrayOf(destBin.drop(1).toByte(2)))
        }
        return bytes
    }
}