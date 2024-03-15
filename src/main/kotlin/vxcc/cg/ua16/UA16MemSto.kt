package vxcc.cg.ua16

import vxcc.cg.*
import vxcc.cg.fake.DefArrayIndexImpl
import vxcc.cg.fake.DefFunOpImpl
import vxcc.cg.fake.DefStaticOpImpl

class UA16MemSto(
    val flags: Owner.Flags,
    val offset: Int = 0,
    val addrGetIntoReg: (String) -> Unit,
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

    fun useAddrInReg(env: UA16Env, block: (UA16Reg) -> Unit) {
        val reg = env.forceAllocReg(env.optimal.ptr, env.firstFreeReg())
        val regSto = reg.storage!!.flatten() as UA16Reg
        addrGetIntoReg(regSto.name)
        if (offset != 0) {
            regSto.emitZero(env)
            regSto.emitStaticAdd(env, offset.toULong(), regSto)
        }
        block(regSto)
        env.dealloc(reg)
    }

    // TODO: bit slices
    override fun reducedStorage(env: UA16Env, flags: Owner.Flags): Storage<UA16Env> =
        UA16MemSto(flags, offset, addrGetIntoReg)

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
                env.memCpy(this, dest, flags.totalWidth)
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

    private fun <V : Value<UA16Env>> binary(env: UA16Env, other: V, dest: Storage<UA16Env>, op: (UA16Reg, Value<UA16Env>, Storage<UA16Env>) -> Unit) {
        useInRegOrSpecific(env, env.clobReg) {
            op(it, if (other == this) it else other, if (dest == this) it else dest)
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
        UA16MemSto(flags, this.offset + offset, addrGetIntoReg)
}