package vxcc.asm

interface Assembler {
    fun label(name: String, flags: Map<String, String?>)
    fun instruction(name: String, args: List<String>, flags: Map<String, String?>)
    fun data(bytes: ByteArray, flags: Map<String, String?>)
}