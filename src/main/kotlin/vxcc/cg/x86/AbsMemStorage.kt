package vxcc.cg.x86

import vxcc.cg.MemStorage
import vxcc.cg.Owner
import vxcc.cg.Storage
import vxcc.cg.Value
import vxcc.cg.fake.DefArrayIndexImpl
import vxcc.cg.fake.DefFunOpImpl
import vxcc.cg.fake.DefStaticOpImpl

class AbsMemStorage(
    val addr: ULong,
    val flags: Owner.Flags
): MemStorage<X86Env>,
    DefFunOpImpl<X86Env>,
    DefStaticOpImpl<X86Env>,
    DefArrayIndexImpl<X86Env>
{
    override fun emitMov(env: X86Env, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitMask(env: X86Env, mask: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: X86Env, new: Owner.Flags): Value<X86Env> {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitAdd(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitMul(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitSignedMul(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitShiftLeft(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitShiftRight(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitExclusiveOr(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun emitShuffle(env: X86Env, selection: IntArray, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun emitZero(env: X86Env) {
        TODO("Not yet implemented")
    }

    override fun offsetBytes(offset: Int): MemStorage<X86Env> {
        TODO("Not yet implemented")
    }

    override fun reducedStorage(env: X86Env, flags: Owner.Flags): Storage<X86Env> {
        TODO("Not yet implemented")
    }
}