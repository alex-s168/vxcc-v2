package vxcc

interface Value {
    /**
     * Move into destination.
     * Truncate if destination smaller
     */
    fun emitMov(env: Env, dest: Storage)

    fun emitStaticMask(env: Env, dest: Storage, mask: Long)

    /**
     * Returns a value object that maps to the lower x bits of the storage.
     * x can not be any value.
     * returned value only exists as long as parent.
     */
    fun reduced(env: Env, to: Int): Value

    fun emitAdd(env: Env, other: Value, dest: Storage)

    fun emitMul(env: Env, other: Value, dest: Storage)

    fun emitSignedMul(env: Env, other: Value, dest: Storage)

    fun emitShiftLeft(env: Env, other: Value, dest: Storage)

    fun emitStaticShiftLeft(env: Env, by: Long, dest: Storage)
}

/** dest = this + index * stride */
fun Owner.emitArrayOffset(env: Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofA(index), stride).emitOffset(env, dest)

/** dest = this + index * stride */
fun Owner.emitArrayOffset(env: Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofB(index), stride).emitOffset(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Owner.emitArrayIndex(env: Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofA(index), stride).emitIndex(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Owner.emitArrayIndex(env: Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofB(index), stride).emitIndex(env, dest)

/** dest = this + index * stride */
fun Value.emitArrayOffset(env: Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofA(index), stride).emitOffset(env, dest)

/** dest = this + index * stride */
fun Value.emitArrayOffset(env: Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofB(index), stride).emitOffset(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Value.emitArrayIndex(env: Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofA(index), stride).emitIndex(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Value.emitArrayIndex(env: Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofB(index), stride).emitIndex(env, dest)

fun Value.emitStaticMultiply(env: Env, by: ULong, dest: Storage) =
    Multiply(this, by.toLong()).emit(env, dest, null)

fun Value.emitStaticMultiply(env: Env, by: ULong, dest: Owner) =
    Multiply(this, by.toLong()).emit(env, dest.storage, dest)