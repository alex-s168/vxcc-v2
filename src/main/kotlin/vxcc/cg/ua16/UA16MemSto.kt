package vxcc.cg.ua16

import vxcc.cg.*
import vxcc.cg.fake.DefArrayIndexImpl
import vxcc.cg.fake.DefFunOpImpl
import vxcc.cg.fake.DefStaticOpImpl
import kotlin.arrayOf

class UA16MemSto(
    val flags: Owner.Flags,
    val offset: Int = 0,
    val addrGetIntoRegIn: (String) -> Unit,
): MemStorage<UA16Env>,
    AbstractScalarValue<UA16Env>,
    DefFunOpImpl<UA16Env>,
    DefStaticOpImpl<UA16Env>,
    DefArrayIndexImpl<UA16Env>
{
    val defer = mutableListOf<() -> Unit>()

    override fun onDestroy(env: UA16Env) {
        defer.forEach { it() }
    }

    fun addrIntoReg(env: UA16Env, reg: UA16Reg) {
        addrGetIntoRegIn(reg.name)
        if (offset != 0) {
            reg.emitStaticAdd(env, offset.toULong(), reg)
        }
    }

    fun useAddrInReg(env: UA16Env, block: (UA16Reg) -> Unit) {
        val reg = env.forceAllocReg(flags, env.firstFreeReg())
        val regSto = reg.storage!!.flatten() as UA16Reg
        addrIntoReg(env, regSto)
        block(regSto)
        env.dealloc(reg)
    }

    // TODO: bit slices
    override fun reducedStorage(env: UA16Env, flags: Owner.Flags): Storage<UA16Env> =
        UA16MemSto(flags, offset, addrGetIntoRegIn)

    override fun emitZero(env: UA16Env) {
        env.emit("clc")
        env.emit("fwc ${env.clobReg}")
        if (flags.totalWidth <= 32) {
            useAddrInReg(env) { addr ->
                repeat(flags.totalWidth / 8) {
                    env.emit("sto ${addr.name}, ${env.clobReg}")
                    env.emit("adc ${addr.name}, 1")
                }
            }
        } else {
            env.memSet(this, UA16Reg(env.clobReg, 16), flags.totalWidth / 8)
        }
    }

    override fun emitMov(env: UA16Env, dest: Storage<UA16Env>) {
        when (dest) {
            is UA16MemSto -> {
                require(dest.flags.totalWidth == flags.totalWidth)
                if (flags.totalWidth in arrayOf(8, 16)) {
                    val temp = env.forceAllocReg(flags, env.firstFreeReg())
                    val tempSto = temp.storage!!.flatten()
                    this.emitMov(env, tempSto)
                    tempSto.emitMov(env, dest)
                    env.dealloc(temp)
                } else {
                    env.memCpy(this, dest, flags.totalWidth)
                }
            }
            else -> {
                require(env.makeRegSize(flags.totalWidth) == flags.totalWidth)
                val oflags = env.flagsOf(dest)
                require(oflags.totalWidth == flags.totalWidth)
                
                useAddrInReg(env) { addrReg ->
                    dest.useInRegWriteBack(env, copyInBegin = false) { dreg ->
                        when (oflags.totalWidth) {
                            8 -> env.emit("lod ${dreg.name}, ${addrReg.name}")
                            16 -> {
                                env.emit("lod ${dreg.name}, ${addrReg.name}")
                                env.unsetCarry()
                                env.emit("adc ${addrReg.name}, 1")
                                env.emit("lod ${dreg.name}, ${addrReg.name}")
                            }
                            else -> throw Exception("wtf")
                        }
                    }
                }
            }
        }
    }

    override fun reduced(env: UA16Env, new: Owner.Flags): Value<UA16Env> =
        reducedStorage(env, new)

    companion object {
        private fun loadOntoStack(env: UA16Env, value: UA16MemSto) {
            require(value.flags.totalWidth in arrayOf(8, 16))
            val valueReg = env.forceAllocReg(value.flags, env.firstFreeReg())
            val valueRegSto = valueReg.storage!!.flatten() as UA16Reg
            value.emitMov(env, valueRegSto)
            env.emit("phr ${valueRegSto.name}")
            env.dealloc(valueReg)
        }

        private fun useStackTopInReg(env: UA16Env, flags: Owner.Flags, block: (UA16Reg) -> Unit) {
            val reg = runCatching { env.forceAllocReg(flags, env.firstFreeReg()) }.getOrNull() ?: env.forceAllocReg(flags, env.clobReg)
            val regSto = reg.storage!!.flatten() as UA16Reg
            env.emit("plr ${regSto.name}")
            block(regSto)
            env.dealloc(reg)
        }
    }

    private fun <V : Value<UA16Env>> binary(env: UA16Env, other: V, dest: Storage<UA16Env>, op: (UA16Reg, Value<UA16Env>, Storage<UA16Env>) -> Unit) {
        if (other is UA16MemSto) {
            loadOntoStack(env, this)
            loadOntoStack(env, other)
            useInRegWriteBack(env, copyInBegin = false) { dreg ->
                useStackTopInReg(env, flags, { b ->
                    useStackTopInReg(env, other.flags, { a ->
                        op(a, b, dreg)
                    })
                })
            }
            return 
        }

        useInRegOrSpecific(env, env.clobReg) {
            op(it, if (other == this) it else other, if (dest == this) it else dest)
            if (dest == this)
                it.emitMov(env, this)
        }
    }

    override fun <V : Value<UA16Env>> emitMask(env: UA16Env, mask: V, dest: Storage<UA16Env>) =
        binary(env, mask, dest) { a, b, o ->
            a.emitMask(env, b, o)
        }

    override fun <V : Value<UA16Env>> emitAdd(env: UA16Env, other: V, dest: Storage<UA16Env>) =
        binary(env, other, dest) { a, b, o ->
            a.emitAdd(env, b, o)
        }

    override fun <V : Value<UA16Env>> emitSub(env: UA16Env, other: V, dest: Storage<UA16Env>) =
        binary(env, other, dest) { a, b, o ->
            a.emitSub(env, b, o)
        }

    override fun <V : Value<UA16Env>> emitMul(env: UA16Env, other: V, dest: Storage<UA16Env>) =
        binary(env, other, dest) { a, b, o ->
            a.emitMul(env, b, o)
        }

    override fun <V : Value<UA16Env>> emitSignedMul(env: UA16Env, other: V, dest: Storage<UA16Env>) =
        binary(env, other, dest) { a, b, o ->
            a.emitSignedMul(env, b, o)
        }

    override fun <V : Value<UA16Env>> emitShiftLeft(env: UA16Env, other: V, dest: Storage<UA16Env>) =
        binary(env, other, dest) { a, b, o ->
            a.emitShiftLeft(env, b, o)
        }

    override fun <V : Value<UA16Env>> emitShiftRight(env: UA16Env, other: V, dest: Storage<UA16Env>) =
        binary(env, other, dest) { a, b, o ->
            a.emitShiftRight(env, b, o)
        }

    override fun <V : Value<UA16Env>> emitExclusiveOr(env: UA16Env, other: V, dest: Storage<UA16Env>) =
        binary(env, other, dest) { a, b, o ->
            a.emitExclusiveOr(env, b, o)
        }

    override fun offsetBytes(offset: Int): MemStorage<UA16Env> =
        UA16MemSto(flags, this.offset + offset, addrGetIntoRegIn)
}
