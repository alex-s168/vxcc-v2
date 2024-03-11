package vxcc.cg.x86

import vxcc.cg.Owner
import vxcc.cg.Type

fun main() {
    val target = Target().apply {
        amd64_v1 = true
        mmx = true
    }

    val env = X86Env(target)
    env.verboseAsm = true

    val a = env.alloc(Owner.Flags(X86Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))
    val b = env.alloc(Owner.Flags(X86Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))
    val o = env.alloc(Owner.Flags(X86Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))

    env.immediate(5, 64).emitMov(env, a.storage)
    env.immediate(20, 64).emitMov(env, b.storage)

    a.emitArrayOffset(env, b, 5, o.storage)

    env.dealloc(a)
    env.dealloc(b)
    env.dealloc(o)
}