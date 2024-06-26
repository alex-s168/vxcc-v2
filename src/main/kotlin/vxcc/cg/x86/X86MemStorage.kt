package vxcc.cg.x86

import vxcc.cg.*
import vxcc.cg.utils.DefFunOpImpl
import vxcc.cg.utils.DefStaticLogicOpImpl
import vxcc.cg.utils.FakeBitSlice
import vxcc.cg.utils.FakeVec
import blitz.flatten

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
    val defer = mutableListOf<() -> Unit>()

    override fun onDestroy(env: X86Env) {
        defer.forEach { it() }
    }

    override fun emitMov(env: X86Env, dest: Storage<X86Env>) {
        when (dest) {
            is MemStorage -> {
                if (dest.getWidth() != flags.totalWidth)
                    error("Can not move into memory location with different size than source! use reducedStorage()")

                val sizes = mutableListOf(8, 16)
                if (env.target.ia32) sizes.add(32)
                if (env.target.amd64_v1) sizes.add(64)
                if (dest.getWidth() in sizes) { // TODO: other reg sizes dependent on target flags
                    val reg = env.forceAllocReg(Owner.Flags(CGEnv.Use.STORE, dest.getWidth(), null, Type.INT))
                    val regSto = reg.storage!!.flatten()
                    emitMov(env, regSto)
                    regSto.emitMov(env, dest)
                    env.dealloc(reg)
                } else {
                    env.memCpy(this, dest, flags.totalWidth)
                }
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

                            else -> error("wtf")
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
        if (dest != this) {
            emitMov(env, dest)
            dest.emitAdd(env, other, dest)
        } else {
            when (other) {
                is Immediate -> if (other.value == 1L)
                        env.emit("  inc ${sizeStr(flags.totalWidth)} [$emit]")
                    else
                        env.emit("  add ${sizeStr(flags.totalWidth)} [$emit], ${other.value}")
                else -> other.useInGPReg(env) {
                    env.emit("  add ${sizeStr(flags.totalWidth)} [$emit], ${it.name}")
                }
            }
        }
    }

    override fun <V : Value<X86Env>> emitSub(env: X86Env, other: V, dest: Storage<X86Env>) {
        if (dest != this) {
            emitMov(env, dest)
            dest.emitAdd(env, other, dest)
        } else {
            when (other) {
                is Immediate -> if (other.value == 1L)
                    env.emit("  dec ${sizeStr(flags.totalWidth)} [$emit]")
                else
                    env.emit("  add ${sizeStr(flags.totalWidth)} [$emit], ${other.value}")
                else -> other.useInGPReg(env) {
                    env.emit("  add ${sizeStr(flags.totalWidth)} [$emit], ${it.name}")
                }
            }
        }
    }

    private fun <V : Value<X86Env>> binaryOp(env: X86Env, other: V, dest: Storage<X86Env>, asm: String, elsefn: () -> Unit) {
        if (dest != this) {
            emitMov(env, dest)
            elsefn()
        } else {
            when (other) {
                is Immediate -> env.emit("  add ${sizeStr(flags.totalWidth)} [$emit], ${other.value}")
                else -> other.useInGPReg(env) {
                    env.emit("  add ${sizeStr(flags.totalWidth)} [$emit], ${it.name}")
                }
            }
        }
    }

    override fun <V : Value<X86Env>> emitMul(env: X86Env, other: V, dest: Storage<X86Env>) =
        binaryOp(env, other, dest, "mul") { dest.emitMul(env, other, dest) }

    override fun <V : Value<X86Env>> emitSignedMul(env: X86Env, other: V, dest: Storage<X86Env>) =
        emitMul(env, other, dest)

    override fun <V : Value<X86Env>> emitShiftLeft(env: X86Env, other: V, dest: Storage<X86Env>) =
        binaryOp(env, other, dest, "shl") { dest.emitShiftLeft(env, other, dest) }

    override fun <V : Value<X86Env>> emitShiftRight(env: X86Env, other: V, dest: Storage<X86Env>) =
        binaryOp(env, other, dest, "shr") { dest.emitShiftRight(env, other, dest) }

    override fun <V : Value<X86Env>> emitExclusiveOr(env: X86Env, other: V, dest: Storage<X86Env>) =
        binaryOp(env, other, dest, "xor") { dest.emitExclusiveOr(env, other, dest) }

    override fun emitShuffle(env: X86Env, selection: IntArray, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    // TODO: overwrite static functions

    override fun emitZero(env: X86Env) {
        env.memSet(this, 0, flags.totalWidth / 8)
    }

    override fun offsetBytes(offset: Int): MemStorage<X86Env> =
        X86MemStorage("$emitBase + ${this.offset + offset}", alignRef + offset, flags, emitBase, this.offset + offset)

    override fun reducedStorage(env: X86Env, flags: Owner.Flags): Storage<X86Env> =
        X86MemStorage(emit, alignRef, flags, emitBase, offset) // TODO: consider weird sizes
}
