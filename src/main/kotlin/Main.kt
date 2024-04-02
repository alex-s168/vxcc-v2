import vxcc.arch.asmForTarget
import vxcc.arch.envForTarget
import vxcc.arch.parseTargetStr
import vxcc.asm.assemble
import vxcc.cg.CGEnv
import java.io.File

fun argParse(argsIn: Array<String>): Map<String, String?> {
    val args = mutableMapOf<String, String?>("" to null)
    var current = ""
    for (a in argsIn) {
        if (a.startsWith("--")) {
            current = a.substring(2)
            args[current] = null
        } else {
            if (args[current] != null)
                error("Unexpected argument: $a")
            args[current] = a
        }
    }
    return args
}

fun main(argsIn: Array<String>) {
    val args = argParse(argsIn)
    if (args.containsKey("help") || args[""] == null) {
        println("vxcc [operation] [options]")
        println("  operations:")
        println("    ir-compile")
        println("    asm")
        println("")
        println("  options:")
        println("    --input [path]        specifies the input file to be processed")
        println("    --target [str]        specifies the target string to compile / assemble for")
        println("    --target-file [path]  specifies the path to a file that contains the target string")
        println("    --origin [addr]       only ua16; compiles / assembles the code to be placed at address [addr]")
        println("    --output [path]       specifies the output path for the binary")
        println("    --opt [mode]          specifies the optimization mode. either \"size\" or \"speed\"")
        println("")
        println("  target:")
        println("    [arch]:[base]:[flags]")
        println("")
        println("    ua16")
        println("      major = \"ua16\"")
        println("      sub = \"\"")
        println("      flags = \"\"")
        println()
        println("    etca")
        println("      major = \"etca\"")
        println("      sub = \"\"")
        println("      flags = semicolon-seperated list of:")
        println("        stack")
        println()
        println("    x86")
        println("      major = \"x86\"")
        println("      sub = \"16\" or \"i386\" or \"i486\" or \"i586\" or \"i686\" or \"amd64\"")
        println("      flags = semicolon-seperated list of:")
        println("        fpu ia32 mmx sce cx8 cmov fxsr osfxsr")
        println("        sse1 sse2 sse3 ssse3 sse4_1 sse4_2")
        println("        amd64_v1 amd64_v2 amd64_v3 amd64_v4")
        println("        cmpxchg16b lahf_sahf popcnt")
        println("        avx avx2 bmi1 bmi2 f16c fma lzcnt")
        println("        movbe osxsave avx512f avx512bw avx512cd")
        println("        avx512dq avx512vl")
        return
    }
    val operation = args[""]!!
    val target = parseTargetStr(args["target-file"]?.let { File(it).readText() } ?: args["target"]!!)

    val opt = args["opt"]?.let {
        when (it) {
            "size" -> CGEnv.OptMode.SIZE
            "speed" -> CGEnv.OptMode.SPEED
            else -> error("Invalid opt mode $it!")
        }
    } ?: CGEnv.OptMode.SPEED
    val input = File(args["input"] ?: error("Input not specified!")).also {
        if (!it.canRead()) error("File not readable!")
    }
    val output = File(args["output"] ?: error("Output not specified!"))
    when (operation) {
        "asm" -> {
            val asm = asmForTarget(target, args["origin"]?.toInt())
            assemble(input.readText(), asm)
            val bytes = asm.finish()
            output.writeBytes(bytes)
        }
        "ir-compile" -> {
            val env = envForTarget(target, args["origin"]?.toInt())
            env.optMode = opt

            val code = input.readText()

            val exception = runCatching {
                env.ir(code.lines().iterator(), verbose = true)
                env.finish()
            }.exceptionOrNull()

            output.writeText(env.source.toString())

            exception?.let { throw it }
        }
        else -> error("Unknown operation")
    }
}
