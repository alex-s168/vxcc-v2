package vxcc.arch

import vxcc.arch.etca.ETCATarget
import vxcc.arch.ua16.UA16Target
import vxcc.arch.x86.X86Target
import vxcc.asm.Assembler
import vxcc.asm.etca.ETCAAssembler
import vxcc.asm.ua16.UA16Assembler
import vxcc.cg.CGEnv
import vxcc.cg.ua16.UA16Env
import vxcc.cg.x86.X86Env

fun parseTargetStr(cpu: String): AbstractTarget {
    val split = cpu.split(':')
    if (split.size != 3)
        throw Exception("Invalid target string! Example: \"x86::amd64_v3,avx512f\"")

    val (tg, sub, feat) = split

    val t = when (tg) {
        "x86" -> X86Target()
        "ua16" -> UA16Target()
        "etca" -> ETCATarget()
        else -> throw Exception("Invalid target major! Available: \"x86\", \"ua16\", \"etca\"")
    }
    t.loadSub(sub)
    t.targetFlags += feat
    return t
}

fun envForTarget(tg: AbstractTarget, origin: Int? = null): CGEnv<*> =
    when (tg) {
        is X86Target -> X86Env(tg)
        is UA16Target -> UA16Env(origin ?: 0, tg)
        is ETCATarget -> TODO("etca codegen")
        else -> throw Exception("wtf")
    }

fun asmForTarget(tg: AbstractTarget, origin: Int? = null): Assembler =
    when (tg) {
        is X86Target -> throw Exception("x86 assembler not yet existent")
        is UA16Target -> UA16Assembler(origin ?: 0, tg)
        is ETCATarget -> ETCAAssembler(origin ?: 0, tg)
        else -> throw Exception("wtf")
    }