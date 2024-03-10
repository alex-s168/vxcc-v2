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
        TODO("Not yet implemented")
    }

    override fun emitStaticMask(env: Env, dest: Storage, mask: Long) {
        TODO("Not yet implemented")
    }

    override fun emitMask(env: Env, mask: Value, dest: Storage) {
        TODO("Not yet implemented")
    }

    override fun reduced(env: Env, to: Int): Value {
        TODO("Not yet implemented")
    }

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