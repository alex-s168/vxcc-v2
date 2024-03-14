package vxcc.cg

interface Value<E: Env<E>> {
    /**
     * Move into destination.
     * Truncate if destination smaller
     */
    fun emitMov(env: E, dest: Storage<E>)

    /**
     * Should be called by the env when deallocating for example
     */
    fun onDestroy(env: E) =
        Unit


    /* ========================= SCALAR ========================== */

    fun emitStaticMask(env: E, mask: Long, dest: Storage<E>)

    fun <V: Value<E>> emitMask(env: E, mask: V, dest: Storage<E>)

    /**
     * Returns a value object that maps to the lower x bits of the storage.
     * x can not be any value.
     * returned value only exists as long as parent.
     */
    fun reduced(env: E, new: Owner.Flags): Value<E>

    fun <V: Value<E>> emitAdd(env: E, other: V, dest: Storage<E>)

    fun emitStaticAdd(env: E, other: ULong, dest: Storage<E>)

    fun <V: Value<E>> emitSub(env: E, other: V, dest: Storage<E>)

    fun emitStaticSub(env: E, other: ULong, dest: Storage<E>)

    fun <V: Value<E>> emitMul(env: E, other: V, dest: Storage<E>)

    fun <V: Value<E>> emitSignedMul(env: E, other: V, dest: Storage<E>)

    fun emitStaticMul(env: E, by: ULong, dest: Storage<E>)

    fun <V: Value<E>> emitShiftLeft(env: E, other: V, dest: Storage<E>)

    fun emitStaticShiftLeft(env: E, by: Long, dest: Storage<E>)

    fun <V: Value<E>> emitShiftRight(env: E, other: V, dest: Storage<E>)

    fun emitStaticShiftRight(env: E, by: Long, dest: Storage<E>)

    fun <V: Value<E>> emitSignedMax(env: E, other: V, dest: Storage<E>)

    fun <V: Value<E>> emitExclusiveOr(env: E, other: V, dest: Storage<E>)

    /** dest = this + index * stride */
    fun <V: Value<E>> emitArrayOffset(env: E, index: V, stride: Long, dest: Storage<E>)

    /** dest = * (typeof(dest) *) (this + index * stride) */
    fun <V: Value<E>> emitArrayIndex(env: E, index: V, stride: Long, dest: Storage<E>)


    /* ========================= VECTOR ========================== */

    fun emitShuffle(env: E, selection: IntArray, dest: Storage<E>)
}