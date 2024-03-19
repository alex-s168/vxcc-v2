package vxcc.arch.x86

import vxcc.cg.AbstractTarget

class X86Target: AbstractTarget() {
    var fpu by flag(false)
    var mmx by flag(false)
    /** also amd64 has this set to true!! */
    var ia32 by flag(true) // TODO: add proper support for 16 bit
    var sse1 by flag(false) { fpu = true }
    var sse2 by flag(false) { sse1 = true }
    var sse3 by flag(false) { sse2 = true }
    var ssse3 by flag(false) { sse3 = true }
    var sse4_1 by flag(false) { sse3 = true }
    var sse4_2 by flag(false) { sse3 = true }
    var sce by flag(false)
    var cx8 by flag(false)
    var cmov by flag(true)
    var fxsr by flag(false)
    var osfxsr by flag(false) { fxsr = true }
    var amd64_v1 by flag(false)  {
        sse2 = true
        sce = true
        cx8 = true
        cmov = true
        osfxsr = true
        mmx = true
    }
    var cmpxchg16b by flag(false)
    var lahf_sahf by flag(false)
    var popcnt by flag(false)
    var amd64_v2 by flag(false) {
        amd64_v1 = true
        cmpxchg16b = true
        lahf_sahf = true
        popcnt = true
        sse3 = true
        ssse3 = true
        sse4_1 = true
        sse4_2 = true
    }
    var avx by flag(false)
    var avx2 by flag(false) { avx = true }
    var bmi1 by flag(false)
    var bmi2 by flag(false)
    var f16c by flag(false)
    var fma by flag(false)
    var lzcnt by flag(false)
    var movbe by flag(false)
    var osxsave by flag(false)
    var amd64_v3 by flag(false) {
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
    var avx512f by flag(false) { avx2 = true }
    var avx512bw by flag(false) { avx512f = true }
    var avx512cd by flag(false) { avx512f = true }
    var avx512dq by flag(false) { avx512f = true }
    var avx512vl by flag(false) { avx512f = true }
    var amd64_v4 by flag(false) {
        amd64_v3 = true
        avx512f = true
        avx512bw = true
        avx512cd = true
        avx512dq = true
        avx512vl = true
    }
}