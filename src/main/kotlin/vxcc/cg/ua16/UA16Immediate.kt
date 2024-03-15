package vxcc.cg.ua16

import vxcc.cg.AbstractScalarValue
import vxcc.cg.Owner
import vxcc.cg.Storage
import vxcc.cg.Value
import vxcc.cg.fake.DefArrayIndexImpl
import vxcc.cg.fake.DefFunOpImpl
import vxcc.cg.fake.DefStaticOpImpl

class UA16Immediate(
    val value: Long,
    val width: Int
): AbstractScalarValue<UA16Env>,
    DefFunOpImpl<UA16Env>,
    DefStaticOpImpl<UA16Env>,
    DefArrayIndexImpl<UA16Env>
{
    override fun emitMov(env: UA16Env, dest: Storage<UA16Env>) {
        dest.useInRegWriteBack(env, copyInBegin = false) { dreg ->
            env.emit("@imm ${dreg.name}, $value")
        }
    }

    override fun <V : Value<UA16Env>> emitMask(env: UA16Env, mask: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: UA16Env, new: Owner.Flags): Value<UA16Env> =
        UA16Immediate(value, new.totalWidth)

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
        TODO("ua16 signed ints")
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
}