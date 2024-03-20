package vxcc.arch.x86

import vxcc.arch.AbstractTarget

class X86Target: AbstractTarget() {
    var fpu by flag()
    var mmx by flag()
    /** also amd64 has this set to true!! */
    var ia32 by flag() // TODO: add proper support for 16 bit
    var sse1 by flag { fpu = true }
    var sse2 by flag { sse1 = true }
    var sse3 by flag { sse2 = true }
    var ssse3 by flag { sse3 = true }
    var sse4_1 by flag { sse3 = true }
    var sse4_2 by flag { sse3 = true }
    var sce by flag()
    var cx8 by flag()
    var cmov by flag()
    var fxsr by flag()
    var osfxsr by flag { fxsr = true }
    var amd64_v1 by flag  {
        ia32 = true
        sse2 = true
        sce = true
        cx8 = true
        cmov = true
        osfxsr = true
        mmx = true
    }
    var cmpxchg16b by flag()
    var lahf_sahf by flag()
    var popcnt by flag()
    var amd64_v2 by flag {
        amd64_v1 = true
        cmpxchg16b = true
        lahf_sahf = true
        popcnt = true
        sse3 = true
        ssse3 = true
        sse4_1 = true
        sse4_2 = true
    }
    var avx by flag()
    var avx2 by flag { avx = true }
    var bmi1 by flag()
    var bmi2 by flag()
    var f16c by flag()
    var fma by flag()
    var lzcnt by flag()
    var movbe by flag()
    var osxsave by flag()
    var amd64_v3 by flag {
        amd64_v2 = true
        avx2 = true
        bmi1 = true
        bmi2 = true
        f16c = true
        fma = true
        lzcnt = true
        movbe = true
        osxsave = true
    }
    var avx512f by flag { avx2 = true }
    var avx512bw by flag { avx512f = true }
    var avx512cd by flag { avx512f = true }
    var avx512dq by flag { avx512f = true }
    var avx512vl by flag { avx512f = true }
    var amd64_v4 by flag {
        amd64_v3 = true
        avx512f = true
        avx512bw = true
        avx512cd = true
        avx512dq = true
        avx512vl = true
    }

    override val subTargets: Map<String, List<String>> = mapOf(
        "16" to listOf(""),
        "i386" to listOf("ia32"),
        "i484" to listOf("ia32", "fpu"),
        "i586" to listOf("ia32", "fpu"),
        "i686" to listOf("ia32", "fpu", "cmov"),
        "amd64" to listOf("amd64_v1")
    )
}