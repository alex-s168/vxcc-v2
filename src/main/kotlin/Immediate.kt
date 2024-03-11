package vxcc

import kotlin.math.max
import kotlin.math.pow

data class Immediate(
    val value: Long,
    val width: Int,
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
        Immediate(value and mask, width).emitMov(env, dest)

    override fun reduced(env: Env, to: Int): Value =
        Immediate(value and (2.0).pow(to).toLong() - 1, to)

    override fun emitAdd(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate(value + other.value, width).emitMov(env, dest)
            else -> other.emitAdd(env, this, dest)
        }

    override fun emitMul(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate((value.toULong() * other.value.toULong()).toLong(), width).emitMov(env, dest)
            else -> other.emitMul(env, this, dest)
        }

    override fun emitSignedMul(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate(value * other.value, width).emitMov(env, dest)
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
        Immediate(value shl by.toInt(), width).emitMov(env, dest)

    override fun emitShiftRight(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> emitStaticShiftRight(env, other.value, dest)
            else -> dest.useInGPRegWriteBack(env, copyInBegin = false) { reg ->
                emitMov(env, reg)
                reg.emitShiftRight(env, other, reg)
            }
        }

    override fun emitStaticShiftRight(env: Env, by: Long, dest: Storage) =
        Immediate(value shl by.toInt(), width).emitMov(env, dest)

    override fun emitSignedMax(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate(max(this.value, other.value), width).emitMov(env, dest)
            else -> other.emitSignedMax(env, this, dest)
        }

    override fun emitExclusiveOr(env: Env, other: Value, dest: Storage) =
        when (other) {
            is Immediate -> Immediate(value xor other.value, width).emitMov(env, dest)
            else -> other.emitExclusiveOr(env, this, dest)
        }

    override fun emitMask(env: Env, mask: Value, dest: Storage) =
        mask.emitMask(env, this, dest) // TODO: can recur!
}