package vxcc.asm

fun parseNum(num: String): Int =
    if (num.startsWith('-'))
        -parseNum(num.substring(1))
    else if (num.startsWith('+'))
        parseNum(num.substring(1))
    else
        if (num.startsWith("h"))
            num.toInt(16)
        else if (num.startsWith("b"))
            num.toInt(2)
        else if (num.startsWith("o"))
            num.toInt(8)
        else
            num.toInt(10)

fun assemble(code: String, asm: Assembler) {
    val lines = code.lines()
    var currentLabel = ""
    val params = mutableListOf<String>()
    fun parseParams(): Map<String, String?> =
        params.associate {
            val sp = it.split(' ', limit = 2)
            sp[0] to sp.getOrNull(1)
        }
    for (lineIn in lines) {
        val line = lineIn.split(";", limit = 2).first().trim()
        if (line.isEmpty())
            continue
        if (line.startsWith('[')) {
            if (!line.endsWith(']'))
                throw Exception("Invalid! Example: [param1, param2, param3]")
            params += line.drop(1).dropLast(1).split(',').map { it.trim() }
        } else if (line.endsWith(':')) {
            var name = line.dropLast(1)
            if (name.startsWith('.')) {
                name = "$currentLabel.$name"
            } else {
                currentLabel = name
            }
            asm.label(name, parseParams())
            params.clear()
        } else if (line.startsWith("db")) {
            val bytes = line.substringAfter("db ")
                .split(',')
                .map { parseNum(it.trim()).toByte() }
                .toByteArray()
            asm.data(bytes, parseParams())
            params.clear()
        } else {
            val split = line.split(' ', limit = 2)
            val cmd = split[0]
            val args = split.getOrNull(1)?.split(',')?.map { it.trim() } ?: listOf()
            asm.instruction(cmd, args, parseParams())
            params.clear()
        }
    }
}