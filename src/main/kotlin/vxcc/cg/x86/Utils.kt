package vxcc.cg.x86

import vxcc.cg.*
import vxcc.cg.fake.FakeBitSlice
import vxcc.cg.fake.FakeVec

fun Value<X86Env>.asReg() =
    this as Reg

fun sizeStr(width: Int) =
    when (width) {
        8 -> "byte"
        16 -> "word"
        32 -> "dword"
        64 -> "qword"
        128 -> "oword"
        else -> throw Exception("Invalid stride (impl error)")
    }

fun Value<X86Env>.getWidth(): Int =
    when (this) {
        is Reg -> this.totalWidth
        is FakeVec -> this.elements.size * this.elemWidth
        is Immediate -> this.width
        is FakeBitSlice -> this.flags.totalWidth
        is X86MemStorage -> this.flags.totalWidth
        else -> TODO("getWidth() for type")
    }

fun Value<X86Env>.useInGPReg(env: X86Env, block: (Reg) -> Unit) =
    useInReg(env, Owner.Flags(Env.Use.STORE, this.getWidth(), null, Type.INT), block)

fun Value<X86Env>.useInReg(env: X86Env, flags: Owner.Flags, block: (Reg) -> Unit) =
    if (this is Reg) {
        block(this)
    } else {
        val reg = env.forceAllocReg(flags)
        val regSto = reg.storage!!.flatten()
        emitMov(env, regSto)
        block(regSto.asReg())
        env.dealloc(reg)
    }

/**
 * If it is not stored in a reg, moves it into a reg
 */
fun Owner<X86Env>.moveIntoReg(env: X86Env) {
    val thisSto = this.storage!!.flatten()
    if (thisSto is Reg)
        return

    // TODO: bugs can occur here
    val new = env.forceAllocReg(flags)
    val newSto = new.storage!!.flatten()
    if (newSto != thisSto)
        thisSto.emitMov(env, newSto)
    this.storage = new.storage
}

/**
 * Moves the first value into a gp reg (if copyInBegin is true (by default true)),
 * then executes the given block if the reg
 * then moves the content of the gp reg into the storage.
 * ((if the storage is not a reg itself))
 */
fun Storage<X86Env>.useInGPRegWriteBack(env: X86Env, copyInBegin: Boolean = true, block: (Reg) -> Unit) =
    useInRegWriteBack(env, Owner.Flags(Env.Use.STORE, this.getWidth(), null, Type.INT), copyInBegin, block)

fun Storage<X86Env>.useInRegWriteBack(env: X86Env, flags: Owner.Flags, copyInBegin: Boolean = true, block: (Reg) -> Unit) =
    if (this is Reg) {
        block(this)
    }
    else {
        val reg = env.forceAllocReg(flags)
        val regSto = reg.storage!!.flatten()
        if (copyInBegin)
            this.emitMov(env, regSto)
        block(regSto.asReg())
        regSto.emitMov(env, this)
        env.dealloc(reg)
    }

/**
 * If either is owner, moves it permanently into a reg (if it is not already)
 * if either is value, moves it into a temp reg (if it is not already)
 */
fun Either<Owner<X86Env>, Value<X86Env>>.useInGPReg(env: X86Env, block: (Reg) -> Unit) =
    this
        .mapA {
            it.moveIntoReg(env)
            block(it.storage!!.flatten().asReg())
        }
        .mapB {
            it.useInGPReg(env, block)
        }