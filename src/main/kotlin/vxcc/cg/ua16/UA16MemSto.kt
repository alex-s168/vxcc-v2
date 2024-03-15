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
        if (offset != 0)
            regSto.emitStaticAdd(env, offset.toULong(), regSto)
        block(regSto)
        env.dealloc(reg)
    }

    // TODO: bit slices
    override fun reducedStorage(env: UA16Env, flags: Owner.Flags): Storage<UA16Env> =
        UA16MemSto(flags, offset, addrGetIntoReg)

    override fun emitZero(env: UA16Env) {
        TODO("Not yet implemented")
    }

    override fun emitMov(env: UA16Env, dest: Storage<UA16Env>) {
        TODO()
    }

    override fun <V : Value<UA16Env>> emitMask(env: UA16Env, mask: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: UA16Env, new: Owner.Flags): Value<UA16Env> =
        reducedStorage(env, new)

    override fun <V : Value<UA16Env>> emitAdd(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> emitSub(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> emitMul(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> emitSignedMul(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> emitShiftLeft(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> emitShiftRight(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> emitExclusiveOr(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun offsetBytes(offset: Int): MemStorage<UA16Env> =
        UA16MemSto(flags, this.offset + offset, addrGetIntoReg)
}