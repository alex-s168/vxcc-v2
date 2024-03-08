package vxcc

class Target {
    var fpu = false
    var mmx = false
    var is32 = true // TODO: add proper support for 16 bit
    var sse1 = false
        set(value) {
            field = value
            fpu = true
        }
    var sse2 = false
        set(value) {
            field = value
            sse1 = true
        }
    var sse3 = false
        set(value) {
            field = value
            sse2 = true
        }
    var ssse3 = false
        set(value) {
            field = value
            sse3 = true
        }
    var sse4_1 = false // TODO: does it infer ssse3?
        set(value) {
            field = value
            sse3 = true
        }
    var sse4_2 = false // TODO: does it infer ssse3?
        set(value) {
            field = value
            sse3 = true
        }
    var sce = false
    var cx8 = false
    var cmov = false
    var fxsr = false
    var osfxsr = false
        set(value) {
            field = value
            fxsr = osfxsr
        }
    var amd64_v1 = false
        set(value) {
            field = value
            sse2 = true
            sce = true
            cx8 = true
            cmov = true
            fxsr = true
            mmx = true
            osfxsr = true
        }
    var cmpxchg16b = false
    var lahf_sahf = false
    var popcnt = false
    var amd64_v2 = false
        set(value) {
            field = value
            amd64_v1 = true
            cmpxchg16b = true
            lahf_sahf = true
            popcnt = true
            sse3 = true
            ssse3 = true
            sse4_1 = true
            sse4_2 = true
        }
    var avx = false
    var avx2 = false
    var bmi1 = false
    var bmi2 = false
    var f16c = false
    var fma = false
    var lzcnt = false
    var movbe = false
    var osxsave = false
    var amd64_v3 = false
        set(value) {
            field = value
            amd64_v2 = true
            avx = true
            avx2 = true
            bmi1 = true
            bmi2 = true
            f16c = true
            fma = true
            lzcnt = true
            movbe = true
            osxsave = true
        }
    var avx512f = false
        set(value) {
            field = value
            avx2 = true
        }
    var avx512bw = false
        set(value) {
            field = value
            avx512f = true
        }
    var avx512cd = false
        set(value) {
            field = value
            avx512f = true
        }
    var avx512dq = false
        set(value) {
            field = value
            avx512f = true
        }
    var avx512vl = false
        set(value) {
            field = value
            avx512f = true
        }
    var amd64_v4 = false
        set(value) {
            field = value
            amd64_v3 = true
            avx512f = true
            avx512bw = true
            avx512cd = true
            avx512dq = true
            avx512vl = true
        }
}