package vxcc

fun main() {
    val target = Target().apply {
        amd64_v1 = true
        mmx = true
    }

    val env = Env(target)
    env.verboseAsm = true

    val a = env.alloc(Owner.Flags(Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))
    val b = env.alloc(Owner.Flags(Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))

    env.immediate(5).emitMov(env, a.storage)

    a.storage.reduced(env, 40).emitMov(env, b.storage.reducedStorage(env, 32))

    env.dealloc(a)
    env.dealloc(b)
}