package vxcc

import kotlin.math.pow

class Immediate(
    val value: Long
): Value {
    override fun emitMov(env: Env, dest: Storage) =
        when (dest) {
            is Reg -> when (dest.type) {
                Reg.Type.GP,
                Reg.Type.GP64EX -> {
                    env.emit("mov ${dest.name}, $value")
                }

                else -> TODO("immediate emit mov to reg type ${dest.type}")
            }

            is Reg.View -> {
                // we can NOT move into zext because it is a view, not a copy of a view!!
                TODO("immediate emit mov to reg view")
                // remove key env from zextMap because need to re-compute
                // this is slow but it is what it is
            }

            else -> TODO("immediate emit mov to $dest")
        }

    override fun emitStaticMask(env: Env, dest: Storage, mask: Long) =
        Immediate(value and mask).emitMov(env, dest)

    override fun reduced(env: Env, to: Int): Value =
        Immediate(value and (2.0).pow(to).toLong() - 1)
}