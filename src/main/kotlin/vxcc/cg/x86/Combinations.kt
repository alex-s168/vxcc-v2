package vxcc.cg.x86

import vxcc.cg.*
import blitz.Either
import blitz.flatten
import kotlin.math.ceil
import kotlin.math.log2

data class X86ArrIndex(
    val arr: Either<Owner<X86Env>, Value<X86Env>>,
    val index: Either<Owner<X86Env>, Value<X86Env>>,
    val stride: Long, // when stride is 1, 2, 4 or 8 -> lea [arr + index * stride], otherwise fma or shifting or mult
) {
    companion object {
        // both regs need to be GP or GP64EX !!!!!!!
        private fun addrSelectorDirect(env: X86Env, arr: Reg, index: Reg, stride: Long) =
            "[${arr.name} + ${index.name} * $stride]"
    }

    internal fun emitOffset(env: X86Env, dest: Storage<X86Env>) {
        if (dest.getWidth() !in arrayOf(32, 64))
            error("Can only emitArrayOffset() or emitArrayIndex() into 32 or 64 bit destinations!")

        when (stride) {
            1L, 2L, 4L, 8L -> {
                dest.useInGPRegWriteBack(env, copyInBegin = false) { reg ->
                    // TODO: if arr and index are not regs, there might be a better way to do this
                    arr.useInGPReg(env) { arrReg ->
                        index.useInGPReg(env) { indexReg ->
                            env.emit(
                                "lea ${reg.name}, ${
                                    addrSelectorDirect(
                                        env,
                                        arrReg,
                                        indexReg,
                                        stride
                                    )
                                }"
                            )
                        }
                    }
                }
            }

            else -> {
                dest.useInGPRegWriteBack(env, copyInBegin = false) { reg ->
                    // TODO: we can do better in some cases
                    X86Multiply(index.mapA { it.storage!!.flatten() }.flatten(), stride).emit(env, reg, null)
                    reg.emitAdd(env, arr.mapA { it.storage!!.flatten() }.flatten(), reg)
                }
            }
        }
    }

    internal fun emitIndex(env: X86Env, dest: Storage<X86Env>) {
        if (dest.getWidth() !in arrayOf(32, 64))
            error("Can only emitArrayOffset() or emitArrayIndex() into 32 or 64 bit destinations!")

        TODO()
    }
}

/*
TODO size opt as described here:
if they're Next to eachother in memory you can Somewhat opt it by doing
  mov rbx, ...
  mov rax, [rbx]
  shl rax, 2
  mov [rbx + ...], rax
which is shorter because you store only an offset the second time and not the whole thing
 */

data class X86Multiply(
    val value: Value<X86Env>,
    val by: Long,
) {
    internal fun emit(env: X86Env, dest: Storage<X86Env>, destOwner: Owner<X86Env>?) {
        if (value is Immediate) {
            env.immediate(value.value * by, dest.getWidth()).emitMov(env, dest)
            return
        }

        val bits = log2(by.toDouble())
        val isPow2 = (ceil(bits) == bits)
        if (by == 0L) {
            dest.emitZero(env)
        }
        else if (by == 1L) {
            if (value != dest)
                value.emitMov(env, dest)
        }
        else if (by == 2L && value == dest) {
            if (dest is Reg) {
                dest.emitAdd(env, dest, dest) // example: add rax, rbx
            } else {
                dest.emitStaticShiftLeft(env, 1, dest) // example: shl [a], 1
            }
        }
        else if (by == 3L || by == 5L || by == 9L) {
            fun doEmit(destReg: Reg) {
                value.useInGPReg(env) { valReg ->
                    env.emit("  lea ${destReg.name}, [${valReg.name} + ${valReg.name} * ${by - 1}]")
                }
            }

            if (destOwner != null) {
                destOwner.moveIntoReg(env)
                doEmit(destOwner.storage!!.flatten().asReg())
            } else {
                dest.useInGPRegWriteBack(env, copyInBegin = false) { destReg ->
                    doEmit(destReg)
                }
            }
        }
        else if (isPow2) {
            fun doEmit(destReg: Reg) {
                if (!(destReg == value || dest.asReg() == value))
                    value.emitMov(env, destReg)
                destReg.emitStaticShiftLeft(env, bits.toLong(), destReg)
            }

            if (destOwner != null) {
                destOwner.moveIntoReg(env)
                doEmit(destOwner.storage!!.flatten().asReg())
            } else {
                dest.useInGPRegWriteBack(env, copyInBegin = false) { destReg ->
                    doEmit(destReg)
                }
            }
        }
        else {
            value.emitMul(env, env.immediate(by, dest.getWidth()), dest)
        }

        // TODO: if speed opt
    }
}