package vxcc

import kotlin.math.ceil
import kotlin.math.log2

private fun Env.emitSize(stride: Long) =
    when (stride) {
        1L -> emit("byte")
        2L -> emit("word")
        4L -> emit("dword")
        8L -> emit("qword")
        16L -> emit("oword")
        else -> throw Exception("Invalid stride (impl error)")
    }

data class ArrIndex(
    val arr: Either<Owner, Value>,
    val index: Either<Owner, Value>,
    val stride: Long, // when stride is 1, 2, 4 or 8 -> lea [arr + index * stride], otherwise fma or shifting or mult
) {
    companion object {
        // both regs need to be GP or GP64EX !!!!!!!
        private fun addrSelectorDirect(env: Env, arr: Reg, index: Reg, stride: Long) =
            "[${arr.name} + ${index.name} * $stride]"
    }

    internal fun emitOffset(env: Env, dest: Storage) {
        if (dest.getWidth() !in arrayOf(32, 64))
            throw Exception("Can only emitArrayOffset() or emitArrayIndex() into 32 or 64 bit destinations!")

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
                    Multiply(index.mapA { it.storage }.commonize(), stride).emit(env, reg, null)
                    reg.emitAdd(env, arr.mapA { it.storage }.commonize(), reg)
                }
            }
        }
    }

    internal fun emitIndex(env: Env, dest: Storage) {
        if (dest.getWidth() !in arrayOf(32, 64))
            throw Exception("Can only emitArrayOffset() or emitArrayIndex() into 32 or 64 bit destinations!")

        TODO()
    }
}

data class Increment(
    val value: Value,
    val by: Long,
) {
    internal fun emit(env: Env, dest: Storage) {
        TODO("Increment:emit()")
    }
}

data class Decrement(
    val value: Value,
    val by: Long,
) {
    internal fun emit(env: Env, dest: Storage) {
        TODO("Decrement:emit()")
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

data class Multiply(
    val value: Value,
    val by: Long,
) {
    internal fun emit(env: Env, dest: Storage, destOwner: Owner?) {
        if (value is Immediate) {
            Immediate(value.value * by).emitMov(env, dest)
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
                    env.emit("lea ${destReg.name}, [${valReg.name} + ${valReg.name} * ${by - 1}]")
                }
            }

            if (destOwner != null) {
                destOwner.moveIntoReg(env)
                doEmit(destOwner.storage.asReg())
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
                doEmit(destOwner.storage.asReg())
            } else {
                dest.useInGPRegWriteBack(env, copyInBegin = false) { destReg ->
                    doEmit(destReg)
                }
            }
        }
        else {
            value.emitMul(env, env.immediate(by), dest)
        }

        // TODO: if speed opt
    }
}