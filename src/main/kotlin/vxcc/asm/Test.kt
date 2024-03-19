package vxcc.asm

import vxcc.arch.etca.ETCATarget
import vxcc.asm.etca.ETCAAssembler

fun main() {
    val asm = ETCAAssembler(0, ETCATarget())
    val code = """
        jmp b
        mov r0, [r1]
        b:
        
    """.trimIndent()
    assemble(code, asm)
    println(asm.finish().joinToString { it.toUByte().toString(2).padStart(8, '0') })
}