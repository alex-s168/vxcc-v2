package vxcc.cg.x86

import vxcc.cg.*
import vxcc.cg.fake.DefFunOpImpl
import vxcc.cg.fake.DefStaticLogicOpImpl
import vxcc.cg.fake.FakeBitSlice
import vxcc.cg.fake.FakeVec

class X86MemStorage(
    val emit: String,
    val alignRef: Long,
    val flags: Owner.Flags,
    val emitBase: String = emit,
    val offset: Int = 0,
): AbstractX86Value,
    MemStorage<X86Env>,
    DefFunOpImpl<X86Env>,
    DefStaticLogicOpImpl<X86Env>,
    PullingStorage<X86Env>
{
    override fun emitMov(env: X86Env, dest: Storage<X86Env>) {
        when (dest) {
            is MemStorage -> {
                if (dest.getWidth() != flags.totalWidth)
                    throw Exception("Can not move into memory location with different size than source! use reducedStorage()")

                env.memCpy(this, dest, flags.totalWidth)
            }

            is PullingStorage<X86Env> -> dest.emitPullFrom(env, this)

            is Reg -> {
                require(dest.totalWidth == flags.totalWidth)

                when (dest.type) {
                    Reg.Type.GP,
                    Reg.Type.GP64EX -> {
                        env.emit("  mov ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                    }

                    Reg.Type.MM -> {
                        env.emit("  movq ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                    }

                    Reg.Type.XMM,
                    Reg.Type.XMM64,
                    Reg.Type.ZMMEX -> {
                        when (dest.totalWidth) {
                            128 ->
                                if (alignRef % 16 == 0L) {
                                    env.emit("  movaps ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                                } else {
                                    env.emit("  movups ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                                }

                            256 ->
                                if (alignRef % 32 == 0L) {
                                    env.emit("  vmovaps ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                                } else {
                                    env.emit("  vmovups ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                                }

                            512 ->
                                if (alignRef % 64 == 0L) {
                                    env.emit("  vmovaps ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                                } else {
                                    env.emit("  vmovups ${dest.name}, ${sizeStr(dest.totalWidth)} [$emit]")
                                }

                            else -> throw Exception("wtf")
                        }
                    }
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
                env.emit("  mov ${sizeStr(flags.totalWidth)} [$emit], ${from.value}")
            }
            is Reg -> {
                if (flags.type.vector) {
                    TODO()
                } else {
                    require(from.isGP())
                    require(from.totalWidth == flags.totalWidth)
                    env.emit("  mov ${sizeStr(flags.totalWidth)} [$emit], ${from.name}")
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
                    env.emit("  mov ${sizeStr(next)} [$emit], ${src.name}")
                }
            }
            else -> TODO()
        }
    }

    override fun <V : Value<X86Env>> emitMask(env: X86Env, mask: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: X86Env, new: Owner.Flags): Value<X86Env> =
        reducedStorage(env, new)

    override fun <V : Value<X86Env>> emitAdd(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitSub(env: X86Env, other: V, dest: Storage<X86Env>) {
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
        env.memSet(this, 0, flags.totalWidth / 8)
    }

    override fun offsetBytes(offset: Int): MemStorage<X86Env> =
        X86MemStorage("$emitBase + ${this.offset + offset}", alignRef + offset, flags, emitBase, this.offset + offset)

    override fun reducedStorage(env: X86Env, flags: Owner.Flags): Storage<X86Env> =
        X86MemStorage(emit, alignRef, flags, emitBase, offset)
}