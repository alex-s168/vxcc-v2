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

    override fun emitAdd(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate(value + other.value).emitMov(env, dest)
            else -> other.emitAdd(env, this, dest)
        }

    override fun emitMul(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate((value.toULong() * other.value.toULong()).toLong()).emitMov(env, dest)
            else -> other.emitMul(env, this, dest)
        }

    override fun emitSignedMul(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate(value.toLong() * other.value.toLong()).emitMov(env, dest)
            else -> other.emitSignedMul(env, this, dest)
        }

    override fun emitShiftLeft(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> emitStaticShiftLeft(env, other.value, dest)
            else -> dest.useInGPRegWriteBack(env, copyInBegin = false) { reg ->
                emitMov(env, reg)
                reg.emitShiftLeft(env, other, reg)
            }
        }

    override fun emitStaticShiftLeft(env: Env, by: Long, dest: Storage) =
        env.immediate(value shl by.toInt()).emitMov(env, dest)
}