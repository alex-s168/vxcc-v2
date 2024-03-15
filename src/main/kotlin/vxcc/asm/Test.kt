package vxcc.asm

import vxcc.asm.ua16.UA16Assembler

fun main() {
    val asm = UA16Assembler(200)
    val code = """
        @imm r0, b
        
        b:
    """.trimIndent()
    assemble(code, asm)
    println(asm.finish().joinToString())
}