import vxcc.cg.Env
import vxcc.cg.Owner
import vxcc.cg.Type
import vxcc.cg.flatten
import vxcc.cg.x86.Target
import vxcc.cg.x86.X86Env

fun main() {
    val target = Target().apply {
        amd64_v1 = true
        mmx = true
    }

    val env = X86Env(target)

    val a = env.alloc(Owner.Flags(Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))
    val b = env.alloc(Owner.Flags(Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))
    val o = env.alloc(Owner.Flags(Env.Use.SCALAR_AIRTHM, 64, null, Type.INT))

    env.immediate(5, 64).emitMov(env, a.storage!!.flatten())
    env.immediate(20, 64).emitMov(env, b.storage!!.flatten())

    a.storage!!.flatten().emitArrayOffset(env, b.storage!!.flatten(), 5, o.storage!!.flatten())

    env.dealloc(a)
    env.dealloc(b)
    env.dealloc(o)
}