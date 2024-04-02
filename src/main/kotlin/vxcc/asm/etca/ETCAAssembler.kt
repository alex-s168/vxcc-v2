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
                            error("Invalid combination of operands")
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

            "push" to Instruction(listOf("stack")) { // TODO: immediate mode
                val a = runCatching { parseReg(it[0]) }.getOrNull()
                if (a != null) { // reg
                    byteBits("00011101")
                    byteBits("110aaa00", "aaa" to a)
                } else { // imm
                    byteBits("01011101")
                    byteBits("110iiiii", "iiiii" to parseNum(it[0]))
                }
            },
            "pop" to Instruction(listOf("stack")) {
                byteBits("00011100")
                byteBits("aaa11000", "aaa" to parseReg(it[0]))
            },

            "rcl" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1000, it) },
            "rcr" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1001, it) },
            "popcnt" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1010, it) },
            "grev" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1011, it) },
            "ctz" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1100, it) },
            "clz" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1101, it) },
            "not" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1110, it) },
            "andn" to Instruction(listOf("bm1")) { simpleEop(0b0_0000_1111, it) },
            "lsb" to Instruction(listOf("bm1")) { simpleEop(0b0_0001_1000, it) },
            "lsmsk" to Instruction(listOf("bm1")) { simpleEop(0b0_0001_1001, it) },
            "rlsb" to Instruction(listOf("bm1")) { simpleEop(0b0_0001_1010, it) },
            "zhib" to Instruction(listOf("bm1")) { simpleEop(0b0_0001_1011, it) },
        )
    }

    override fun instruction(name: String, args: List<String>, flags: Map<String, String?>) {
        try {
            super.instruction(name, args, flags)
        } catch (e: Exception) {
            if (name.startsWith("jmp")) {
                val cond = parseCond(name.substring(3))

                if (args[0].startsWith('[')) {
                    if ("stack" !in target.targetFlags)
                        error("Required extension \"stack\" for absolute register jmp!")
                    val reg = parseReg(args[0].drop(1).dropLast(1))
                    byteBits("10101111")
                    byteBits("rrr0cccc", "rrr" to reg, "cccc" to cond)
                } else {
                    jumpOp(cond, args)
                }
            } else if (name.startsWith("call")) {
                if (args[0].startsWith('[')) {
                    val cond = parseCond(name.substring(3))
                    val reg = parseReg(args[0].drop(1).dropLast(1))

                    byteBits("10101111")
                    byteBits("rrr1cccc", "rrr" to reg, "cccc" to cond)
                } else {
                    if (name != "call")
                        error("Conditional calls only supported for absolute register calls!")

                    TODO("relative calls")
                }
            } else {
                throw e
            }
        }
    }

    fun parseCond(cond: String): Int =
        when (cond) {
            ".z" -> 0b0000
            ".e" -> 0b0000
            ".nz" ->0b0001
            ".ne" ->0b0001
            ".n" -> 0b0010
            ".nn" -> error("Use jmp.p instead of jmp.nn!")
            ".p" -> 0b0011
            ".c" -> 0b0100
            ".b" -> 0b0100
            ".nc" ->0b0101
            ".ae" ->0b0101
            ".o" -> 0b0110
            ".no" ->0b0111
            ".be" ->0b1000
            ".a" -> 0b1001
            ".l" -> 0b1010
            ".ge" ->0b1011
            ".le" ->0b1100
            ".g" -> 0b1101
            ".never" -> 0b1111
            "" -> 0b1110
            else -> error("Not a condition suffix $cond")
        }

    fun parseCR(reg: String): Int =
        when (reg) {
            "CPUID1" -> 0
            "CPUID2" -> 1
            "FEAT" -> 2
            "FLAGS" -> requireFeat("int", 3)
            "INT_PC"-> requireFeat("int", 4)
            "INT_RET_PC"-> requireFeat("int", 5)
            "INT_MASK"-> requireFeat("int", 6)
            "INT_PENDING"-> requireFeat("int", 7)
            "INT_CAUSE"-> requireFeat("int", 8)
            "INT_DATA"-> requireFeat("int", 9)
            "INT_SCRATCH_0"-> requireFeat("int", 10)
            "INT_SCRATCH_1"-> requireFeat("int", 11)
            else -> error("Unknown control register $reg")
        }

    fun parseReg(reg: String): Int {
        if (reg.startsWith('r')) {
            val id = reg.substring(1).toUIntOrNull()
                ?: error("Invalid register!")

            if (id > 7u)
                error("Invalid register!")

            return when (id) {
                5u -> error("r5 should be referred to as bp to avoid confusion")
                6u -> error("r6 should be referred to as sp to avoid confusion")
                7u -> error("r7 should be referred to as ln to avoid confusion")
                else -> id.toInt()
            }
        }

        return when (reg) {
            "bp" -> 5
            "sp" -> 6
            "ln" -> 7
            else -> error("Invalid register!")
        }
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

    fun eopRR(opcode: Int, size: Int, reg1: Int, reg2: Int, memMode: Int) {
        byteBits("1110CCCC", "CCCC" to opcode.shr(5))
        byteBits("C0SSEEEE", "C" to opcode.shr(4).and(1), "EEEE" to opcode.and(0b1111), "SS" to size)
        byteBits("AAABBBMM", "AAA" to reg1, "BBB" to reg2, "MM" to memMode)
    }

    fun eopRI(opcode: Int, size: Int, reg: Int, imm: Int) {
        byteBits("1110CCCC", "CCCC" to opcode.shr(5))
        byteBits("C1SSEEEE", "C" to opcode.shr(4).and(1), "EEEE" to opcode.and(0b1111), "SS" to size)
        byteBits("RRRIIIII", "RRR" to reg, "IIIII" to imm)
    }

    fun simpleEop(opcode: Int, args: List<String>) {
        val a = parseReg(args[0])
        val b = kotlin.runCatching { parseReg(args[1]) }.getOrNull()
        if (b != null) { // reg reg
            eopRR(opcode, 1, a, b, 0)
        } else { // reg imm
            val imm = parseNum(args[1])
            eopRI(opcode, 1, a, imm)
        }
    }

    override fun finish(): ByteArray {
        for (ref in refs) { // relative
            next = ref.where
            val dest = parseLabelOrNum(ref.dest) - ref.where
            if (dest > Byte.MAX_VALUE * 2)
                error("jump rel9 too big")
            if (dest < Byte.MIN_VALUE * 2)
                error("jump rel9 too small")

            val destBin = dest.toString(2).padStart(9, '0')

            byteBits("100dcccc", "d" to "${destBin[0]}".toInt(2), "cccc" to ref.opcode)
            data(byteArrayOf(destBin.drop(1).toByte(2)))
        }
        return bytes
    }
}