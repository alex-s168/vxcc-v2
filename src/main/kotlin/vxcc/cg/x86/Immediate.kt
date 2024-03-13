package vxcc.cg.x86

import vxcc.cg.*
import kotlin.math.max
import kotlin.math.pow

// TODO: lots of things can recur!

data class Immediate(
    val value: Long,
    val width: Int,
): AbstractX86Value(), AbstractScalarValue<X86Env> {
    override fun emitMov(env: X86Env, dest: Storage<X86Env>) =
        when (dest) {
            is Reg -> when (dest.type) {
                Reg.Type.GP,
                Reg.Type.GP64EX -> {
                    env.emit("mov ${dest.name}, $value")
                }

                else -> TODO("immediate emit mov to reg type ${dest.type}")
            }
            is PullingStorage -> dest.emitPullFrom(env, this)
            else -> TODO("immediate emit mov to $dest")
        }

    override fun emitStaticMask(env: X86Env, mask: Long, dest: Storage<X86Env>) =
        Immediate(value and mask, width).emitMov(env, dest)

    override fun reduced(env: X86Env, new: Owner.Flags): Value<X86Env> =
        Immediate(value and (2.0).pow(new.totalWidth).toLong() - 1, new.totalWidth)

    override fun <V: Value<X86Env>> emitAdd(env: X86Env, other: V, dest: Storage<X86Env>) =
        when (other) {
            is Immediate -> Immediate(value + other.value, width).emitMov(env, dest)
            else -> other.emitAdd(env, this, dest)
        }

    override fun <V: Value<X86Env>> emitMul(env: X86Env, other: V, dest: Storage<X86Env>) =
        when (other) {
            is Immediate -> Immediate((value.toULong() * other.value.toULong()).toLong(), width).emitMov(env, dest)
            else -> other.emitMul(env, this, dest)
        }

    override fun <V: Value<X86Env>> emitSignedMul(env: X86Env, other: V, dest: Storage<X86Env>) =
        when (other) {
            is Immediate -> Immediate(value * other.value, width).emitMov(env, dest)
            else -> other.emitSignedMul(env, this, dest)
        }

    override fun <V: Value<X86Env>> emitShiftLeft(env: X86Env, other: V, dest: Storage<X86Env>) =
        when (other) {
            is Immediate -> emitStaticShiftLeft(env, other.value, dest)
            else -> dest.useInGPRegWriteBack(env, copyInBegin = false) { reg ->
                emitMov(env, reg)
                reg.emitShiftLeft(env, other, reg)
            }
        }

    override fun emitStaticShiftLeft(env: X86Env, by: Long, dest: Storage<X86Env>) =
        Immediate(value shl by.toInt(), width).emitMov(env, dest)

    override fun <V: Value<X86Env>> emitShiftRight(env: X86Env, other: V, dest: Storage<X86Env>) =
        when (other) {
            is Immediate -> emitStaticShiftRight(env, other.value, dest)
            else -> dest.useInGPRegWriteBack(env, copyInBegin = false) { reg ->
                emitMov(env, reg)
                reg.emitShiftRight(env, other, reg)
            }
        }

    override fun emitStaticShiftRight(env: X86Env, by: Long, dest: Storage<X86Env>) =
        Immediate(value shl by.toInt(), width).emitMov(env, dest)

    override fun <V: Value<X86Env>> emitSignedMax(env: X86Env, other: V, dest: Storage<X86Env>) =
        when (other) {
            is Immediate -> Immediate(max(this.value, other.value), width).emitMov(env, dest)
            else -> other.emitSignedMax(env, this, dest)
        }

    override fun <V: Value<X86Env>> emitExclusiveOr(env: X86Env, other: V, dest: Storage<X86Env>) =
        when (other) {
            is Immediate -> Immediate(value xor other.value, width).emitMov(env, dest)
            else -> other.emitExclusiveOr(env, this, dest)
        }

    override fun <V: Value<X86Env>> emitMask(env: X86Env, mask: V, dest: Storage<X86Env>) =
        mask.emitMask(env, this, dest)
}