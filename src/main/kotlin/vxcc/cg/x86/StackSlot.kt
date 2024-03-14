package vxcc.cg.x86

import vxcc.cg.*

class StackSlot(
    val spOff: Long,
    val width: Int,
): AbstractX86Value(), MemStorage<X86Env> {
    var vecElemWidth: Int? = null

    private fun spRegName(env: X86Env) =
        if (env.target.amd64_v1) "rsp"
        else "esp"

    internal fun spIndexStr(env: X86Env) =
        if (spOff < 0)
            "${spRegName(env)} - $spOff"
        else
            "${spRegName(env)} + $spOff"

    override fun reducedStorage(env: X86Env, flags: Owner.Flags): Storage<X86Env> =
        StackSlot(spOff, flags.totalWidth).also { it.vecElemWidth = vecElemWidth }

    override fun offsetBytes(offset: Int): MemStorage<X86Env> =
        StackSlot(spOff + offset, width).also { it.vecElemWidth = vecElemWidth }

    override fun emitZero(env: X86Env) {
        env.memSet(this, 0, width)
    }

    override fun emitMov(env: X86Env, dest: Storage<X86Env>) {
        when (dest) {
            is Reg -> {
                if (dest.totalWidth != width)
                    throw Exception("Can not move into register with different size that source! use reduced()")

                when (dest.type) {
                    Reg.Type.GP,
                    Reg.Type.GP64EX -> {
                        env.emit("  mov ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                    }

                    Reg.Type.MM -> {
                        env.emit("  movq ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                    }

                    Reg.Type.XMM,
                    Reg.Type.XMM64,
                    Reg.Type.ZMMEX -> {
                        when (dest.totalWidth) {
                            128 ->
                                if (spOff % 16 == 0L) {
                                    env.emit("  movaps ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                } else {
                                    env.emit("  movups ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                }

                            256 ->
                                if (spOff % 32 == 0L) {
                                    env.emit("  vmovaps ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                } else {
                                    env.emit("  vmovups ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                }

                            512 ->
                                if (spOff % 64 == 0L) {
                                    env.emit("  vmovaps ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                } else {
                                    env.emit("  vmovups ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                }

                            else -> throw Exception("wtf")
                        }
                    }
                }
            }
            is PullingStorage -> dest.emitPullFrom(env, this)
            is MemStorage -> {
                if (dest.getWidth() != width)
                    throw Exception("Can not move into memory location with different size than source! use reducedStorage()")

                env.memCpy(this, dest, width)
            }
            else -> TODO("stack slot move to $dest")
        }
    }

    override fun emitStaticMask(env: X86Env, mask: Long, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V: Value<X86Env>> emitMask(env: X86Env, mask: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: X86Env, new: Owner.Flags): Value<X86Env> =
        StackSlot(spOff, new.totalWidth).also { it.vecElemWidth = vecElemWidth }

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

    override fun emitStaticShiftLeft(env: X86Env, by: Long, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitShiftRight(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun emitStaticShiftRight(env: X86Env, by: Long, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitSignedMax(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<X86Env>> emitExclusiveOr(env: X86Env, other: V, dest: Storage<X86Env>) {
        TODO("Not yet implemented")
    }

    override fun emitShuffle(env: X86Env, selection: IntArray, dest: Storage<X86Env>) {
        require(this.vecElemWidth != null)
        TODO("use in vec reg")
    }
}