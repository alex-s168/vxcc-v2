package vxcc.cg.ua16

import vxcc.cg.*
import vxcc.cg.utils.DefArrayIndexImpl
import vxcc.cg.utils.DefFunOpImpl
import vxcc.cg.utils.DefStaticOpImpl

class UA16Reg(
    val name: String,
    val width: Int,
): Storage<UA16Env>,
    AbstractScalarValue<UA16Env>,
    DefFunOpImpl<UA16Env>,
    DefStaticOpImpl<UA16Env>,
    DefArrayIndexImpl<UA16Env>
{
    override fun emitMov(env: UA16Env, dest: Storage<UA16Env>) {
        when (dest) {
            is UA16MemSto -> dest.useAddrInReg(env) { addr ->
                when (width) {
                    8 -> env.emit("sto ${addr.name}, $name")
                    16 -> {
                        env.emit("sto ${addr.name}, $name")
                        env.unsetCarry()
                        env.emit("adc ${addr.name}, 1")
                        env.emit("sto ${addr.name}, $name")
                    }
                    else -> error("wtf")
                }
            }
            else -> dest.useInRegWriteBack(env, copyInBegin = false) { dreg ->
                env.emit("mov ${dreg.name}, $name")
            }
        }
    }

    override fun <V : Value<UA16Env>> emitMask(env: UA16Env, mask: V, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: UA16Env, new: Owner.Flags): Value<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> emitAdd(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        if (dest != this) {
            // TODO: might cause problems
            other.emitMov(env, dest)
            dest.emitAdd(env, this, dest)
            return
        }

        when (other) {
            is UA16Immediate -> {
                if (other.value <= (if (env.optMode == CGEnv.OptMode.SIZE) 8 else 4)) {
                    env.unsetCarry()
                    repeat(other.value.toInt()) {
                        env.emit("adc $name, 1")
                    }
                } else {
                    other.useInReg(env) { reg ->
                        emitAdd(env, reg, this)
                    }
                }
            }
            is UA16Reg -> {
                env.unsetCarry()
                env.emit("adc $name, ${other.name}")
            }
            else -> other.useInReg(env) { reg ->
                emitAdd(env, reg, this)
            }
        }
    }

    override fun <V : Value<UA16Env>> emitSub(env: UA16Env, other: V, dest: Storage<UA16Env>) {
        if (dest != this) {
            TODO()
            return
        }

        when (other) {
            is UA16Immediate -> {
                if (other.value <= (if (env.optMode == CGEnv.OptMode.SIZE) 8 else 4)) {
                    repeat(other.value.toInt()) {
                        env.emit("@sbc $name, 1, clob=${env.clobReg}")
                    }
                } else {
                    other.useInReg(env) { reg ->
                        emitAdd(env, reg, this)
                    }
                }
            }
            is UA16Reg -> {
                env.emit("@sbc $name, ${other.name}, clob=${env.clobReg}")
            }
            else -> other.useInReg(env) { reg ->
                emitAdd(env, reg, this)
            }
        }
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

    override fun reducedStorage(env: UA16Env, flags: Owner.Flags): Storage<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun emitZero(env: UA16Env) {
        env.emit("clc")
        env.emit("fwc $name")
    }
}
