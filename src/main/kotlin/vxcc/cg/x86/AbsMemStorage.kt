package vxcc.cg.x86

import vxcc.cg.*
import vxcc.cg.fake.DefFunOpImpl
import vxcc.cg.fake.DefStaticLogicOpImpl
import vxcc.cg.fake.FakeBitSlice
import vxcc.cg.fake.FakeVec

class AbsMemStorage(
    val addr: ULong,
    val flags: Owner.Flags
): AbstractX86Value,
    MemStorage<X86Env>,
    DefFunOpImpl<X86Env>,
    DefStaticLogicOpImpl<X86Env>,
    PullingStorage<X86Env>
{
    override fun emitMov(env: X86Env, dest: Storage<X86Env>) {
        when (dest) {
            is PullingStorage<X86Env> -> dest.emitPullFrom(env, this)
            is Reg -> {
                if (flags.type.vector) {
                    TODO()
                } else {
                    require(dest.isGP())
                    require(dest.totalWidth == flags.totalWidth)
                    env.emit("  mov ${dest.name}, ${sizeStr(flags.totalWidth)} [$addr]")
                }
            }
            else -> TODO()
        }
    }

    override fun emitPullFrom(env: X86Env, from: Value<X86Env>) {
        when (from) {
            is Immediate -> {
                require(flags.totalWidth == from.width)
                require(!flags.type.vector)
                env.emit("  mov ${sizeStr(flags.totalWidth)} [$addr], ${from.value}")
            }
            is Reg -> {
                if (flags.type.vector) {
                    TODO()
                } else {
                    require(from.isGP())
                    require(from.totalWidth == flags.totalWidth)
                    env.emit("  mov ${sizeStr(flags.totalWidth)} [$addr], ${from.name}")
                }
            }
            is FakeVec -> {
                require(flags.type.vector)
                TODO()
            }
            is FakeBitSlice -> {
                require(!flags.type.vector)
                require(from.flags.totalWidth == flags.totalWidth)
                val next = env.makeRegSize(flags.totalWidth)
                from.zeroExtended
                    .computeIfAbsent(env, from::compute)
                    .storage!!.flatten()
                    .useInGPReg(env) { src ->
                    env.emit("  mov ${sizeStr(next)} [$addr], ${src.name}")
                }
            }
            else -> TODO()
        }
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