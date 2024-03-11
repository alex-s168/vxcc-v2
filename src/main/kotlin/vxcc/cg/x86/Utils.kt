package vxcc.cg.x86

import vxcc.cg.Either
import vxcc.cg.Owner
import vxcc.cg.Storage

fun sizeStr(width: Int) =
    when (width) {
        8 -> "byte"
        16 -> "word"
        32 -> "dword"
        64 -> "qword"
        128 -> "oword"
        else -> throw Exception("Invalid stride (impl error)")
    }

fun Value.getWidth(): Int =
    when (this) {
        is Reg -> this.totalWidth
        is Reg.View -> this.size
        is Immediate -> this.width
        is StackSlot -> this.width
        else -> TODO("getWidth() for type")
    }

/**
 * SHOULD NOT BE USED IN MOST CASES!
 */
fun Value.useInGPReg(env: X86Env, block: (Reg) -> Unit) =
    if (this is Reg) {
        block(this)
    } else {
        val reg = env.forceAllocReg(Owner.Flags(X86Env.Use.STORE, this.getWidth(), null, Type.INT))
        emitMov(env, reg.storage)
        block(reg.storage.asReg())
        env.dealloc(reg)
    }

/**
 * If it is not stored in a reg, moves it into a reg
 */
fun Owner.moveIntoReg(env: X86Env) {
    if (this.storage is Reg)
        return

    // TODO: bugs can occur here
    val new = env.forceAllocReg(flags)
    if (new.storage != this.storage)
        this.storage.emitMov(env, new.storage)
    this.storage = new.storage
}

/**
 * Moves the first value into a gp reg (if copyInBegin is true (by default true)),
 * then executes the given block if the reg
 * then moves the content of the gp reg into the storage.
 * ((if the storage is not a reg itself))
 */
fun Storage.useInGPRegWriteBack(env: X86Env, copyInBegin: Boolean = true, block: (Reg) -> Unit) =
    if (this is Reg) {
        block(this)
    }
    else {
        val reg = env.forceAllocReg(Owner.Flags(X86Env.Use.STORE, this.getWidth(), null, Type.INT))
        if (copyInBegin)
            this.emitMov(env, reg.storage)
        block(reg.storage.asReg())
        reg.storage.emitMov(env, this)
        env.dealloc(reg)
    }

/**
 * If either is owner, moves it permanently into a reg (if it is not already)
 * if either is value, moves it into a temp reg (if it is not already)
 */
fun Either<Owner, Value>.useInGPReg(env: X86Env, block: (Reg) -> Unit) =
    this
        .mapA {
            it.moveIntoReg(env)
            block(it.storage.asReg())
        }
        .mapB {
            it.useInGPReg(env, block)
        }