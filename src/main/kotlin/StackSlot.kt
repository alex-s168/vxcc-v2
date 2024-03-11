package vxcc

class StackSlot(
    val spOff: Long,
    val width: Int,
): MemStorage {
    private fun spRegName(env: Env) =
        if (env.target.amd64_v1) "rsp"
        else "esp"

    private fun spIndexStr(env: Env) =
        if (spOff < 0)
            "${spRegName(env)} - $spOff"
        else
            "${spRegName(env)} + $spOff"

    override fun reducedStorage(env: Env, to: Int): Storage =
        StackSlot(spOff, to)

    override fun offsetBytes(offset: Int): MemStorage =
        StackSlot(spOff + offset, width)

    override fun emitZero(env: Env) {
        memSet(env, this, 0, width)
    }

    override fun emitMov(env: Env, dest: Storage) {
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

    override fun emitStaticMask(env: Env, dest: Storage, mask: Long) {
        TODO("Not yet implemented")
    }

    override fun emitMask(env: Env, mask: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: Env, to: Int): Value =
        StackSlot(spOff, width)

    override fun emitAdd(env: Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitMul(env: Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitSignedMul(env: Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitShiftLeft(env: Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitStaticShiftLeft(env: Env, by: Long, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitShiftRight(env: Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitStaticShiftRight(env: Env, by: Long, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitSignedMax(env: Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun emitExclusiveOr(env: Env, other: Value, dest: Storage) {
        TODO("Not yet implemented")
    }
}