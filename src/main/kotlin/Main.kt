import vxcc.asm.assemble
import vxcc.asm.ua16.UA16Assembler
import vxcc.cg.Env
import vxcc.cg.ua16.UA16Env
import vxcc.cg.ua16.UA16Target
import vxcc.cg.x86.X86Env
import vxcc.cg.x86.X86Target
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
                throw Exception("Unexpected argument: $a")
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
        println("    [major]:[sub]:[flags]")
        println("")
        println("    ua16")
        println("      major = \"ua16\"")
        println("      sub = \"\"")
        println("      flags = \"\"")
        println()
        println("    x86")
        println("      major = \"x86\"")
        println("      sub = \"\" or \"16\" or \"pentium\" or \"amd64\"")
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
    val (targetMajor, targetSub, targetFlags) = (args["target-file"]?.let { File(it).readText() } ?: args["target"]!!).split(":", limit = 3)
    val target = when (targetMajor) {
        "ua16" -> UA16Target().also {
            if (targetSub != "")
                throw Exception("Unknown target sub for ua16!")
        }
        "x86" -> X86Target().also {
            when (targetSub) {
                "" -> Unit
                "16" -> it.ia32 = false
                "pentium" -> {
                    it.ia32 = true
                    it.fpu = true
                }
                "amd64" -> {
                    it.amd64_v1 = true
                }
                else -> throw Exception("Unknown target sub for x86!")
            }
        }
        else -> throw Exception("Unknown target $targetMajor!")
    }
    target.targetFlags += targetFlags.split(';')
    val opt = args["opt"]?.let {
        when (it) {
            "size" -> Env.OptMode.SIZE
            "speed" -> Env.OptMode.SPEED
            else -> throw Exception("Invalid opt mode $it!")
        }
    } ?: Env.OptMode.SPEED
    val input = File(args["input"] ?: throw Exception("Input not specified!")).also {
        if (!it.canRead()) throw Exception("File not readable!")
    }
    val output = File(args["output"] ?: throw Exception("Output not specified!"))
    when (operation) {
        "asm" -> {
            val asm = when (targetMajor) {
                "ua16" -> UA16Assembler(args["origin"]?.toInt() ?: 0)
                else -> throw Exception("Target does not support assembler!")
            }
            assemble(input.readText(), asm)
            val bytes = asm.finish()
            output.writeBytes(bytes)
        }
        "ir-compile" -> {
            val env = when (targetMajor) {
                "ua16" -> UA16Env(args["origin"]?.toInt() ?: 0)
                "x86" -> X86Env(target as X86Target)
                else -> throw Exception("Target does not support codegen!")
            }
            env.optMode = opt

            val code = input.readText()

            val exception = runCatching {
                env.ir(code.lines().iterator(), verbose = true)
                env.finish()
            }.exceptionOrNull()

            output.writeText(env.source.toString())

            exception?.let { throw it }
        }
        else -> throw Exception("Unknown operation")
    }
}
