package vxcc.cg.x86

import vxcc.cg.Storage

class StackSlot(
    val spOff: Long,
    val width: Int,
): MemStorage {
    private fun spRegName(env: X86Env) =
        if (env.target.amd64_v1) "rsp"
        else "esp"

    private fun spIndexStr(env: X86Env) =
        if (spOff < 0)
            "${spRegName(env)} - $spOff"
        else
            "${spRegName(env)} + $spOff"

    override fun reducedStorage(env: X86Env, to: Int): Storage =
        StackSlot(spOff, to)

    override fun offsetBytes(offset: Int): MemStorage =
        StackSlot(spOff + offset, width)

    override fun emitZero(env: X86Env) {
        memSet(env, this, 0, width)
    }

    override fun emitMov(env: X86Env, dest: Storage) {
        when (dest) {
            is Reg -> {
                if (dest.totalWidth != width)
                    throw Exception("Can not move into register with different size that source! use reduced()")

                when (dest.type) {
                    Reg.Type.GP,
                    Reg.Type.GP64EX -> {
                        env.emit("mov ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                    }

                    Reg.Type.MM -> {
                        env.emit("movq ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                    }

                    Reg.Type.XMM,
                    Reg.Type.XMM64,
                    Reg.Type.ZMMEX -> {
                        when (dest.totalWidth) {
                            128 ->
                                if (spOff % 16 == 0L) {
                                    env.emit("movaps ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                } else {
                                    env.emit("movups ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                }

                            256 ->
                                if (spOff % 32 == 0L) {
                                    env.emit("vmovaps ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                } else {
                                    env.emit("vmovups ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                }

                            512 ->
                                if (spOff % 64 == 0L) {
                                    env.emit("vmovaps ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                } else {
                                    env.emit("vmovups ${dest.name}, ${sizeStr(dest.totalWidth)} [${spIndexStr(env)}]")
                                }

                            else -> throw Exception("wtf")
                        }
                    }
                }
            }

            is Reg.View -> {
                if (dest.size != width)
                    throw Exception("Can not move into register (view) with different size that source! use reduced()")

                if (dest.reg.type == Reg.Type.MM && dest.size == 32) {
                    env.emit("movd ${dest.reg.name}, ${sizeStr(32)} [${spIndexStr(env)}]")
                } else {
                    TODO("mov stack slot into reg view")
                }
            }

            is MemStorage -> {
                if (dest.getWidth() != width)
                    throw Exception("Can not move into memory location with different size than source! use reducedStorage()")

                memCpy(env, this, dest, width)
            }

            else -> TODO("stack slot move to $dest")
        }
    }

    override fun emitStaticMask(env: X86Env, dest: Storage, mask: Long) {
        TODO("Not yet implemented")
    }

    override fun emitMask(env: X86Env, mask: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: X86Env, to: Int): Value =
        StackSlot(spOff, width)

    override fun emitAdd(env: X86Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitMul(env: X86Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitSignedMul(env: X86Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitShiftLeft(env: X86Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitStaticShiftLeft(env: X86Env, by: Long, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitShiftRight(env: X86Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitStaticShiftRight(env: X86Env, by: Long, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitSignedMax(env: X86Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitExclusiveOr(env: X86Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }
}