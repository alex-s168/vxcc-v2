package vxcc.vxcc.x86

interface Value {
    /**
     * Move into destination.
     * Truncate if destination smaller
     */
    fun emitMov(env: X86Env, dest: Storage)

    fun emitStaticMask(env: X86Env, dest: Storage, mask: Long)

    fun emitMask(env: X86Env, mask: Value, dest: Storage)

    /**
     * Returns a value object that maps to the lower x bits of the storage.
     * x can not be any value.
     * returned value only exists as long as parent.
     */
    fun reduced(env: X86Env, to: Int): Value

    fun emitAdd(env: X86Env, other: Value, dest: Storage)

    fun emitMul(env: X86Env, other: Value, dest: Storage)

    fun emitSignedMul(env: X86Env, other: Value, dest: Storage)

    fun emitShiftLeft(env: X86Env, other: Value, dest: Storage)

    fun emitStaticShiftLeft(env: X86Env, by: Long, dest: Storage)

    fun emitShiftRight(env: X86Env, other: Value, dest: Storage)

    fun emitStaticShiftRight(env: X86Env, by: Long, dest: Storage)

    fun emitSignedMax(env: X86Env, other: Value, dest: Storage)

    fun emitExclusiveOr(env: X86Env, other: Value, dest: Storage)
}

/** dest = this + index * stride */
fun Owner.emitArrayOffset(env: X86Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofA(index), stride).emitOffset(env, dest)

/** dest = this + index * stride */
fun Owner.emitArrayOffset(env: X86Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofB(index), stride).emitOffset(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Owner.emitArrayIndex(env: X86Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofA(index), stride).emitIndex(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Owner.emitArrayIndex(env: X86Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofA(this), Either.ofB(index), stride).emitIndex(env, dest)

/** dest = this + index * stride */
fun Value.emitArrayOffset(env: X86Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofA(index), stride).emitOffset(env, dest)

/** dest = this + index * stride */
fun Value.emitArrayOffset(env: X86Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofB(index), stride).emitOffset(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Value.emitArrayIndex(env: X86Env, index: Owner, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofA(index), stride).emitIndex(env, dest)

/** dest = * (typeof(dest) *) (this + index * stride) */
fun Value.emitArrayIndex(env: X86Env, index: Value, stride: Long, dest: Storage) =
    ArrIndex(Either.ofB(this), Either.ofB(index), stride).emitIndex(env, dest)

fun Value.emitStaticMultiply(env: X86Env, by: ULong, dest: Storage) =
    Multiply(this, by.toLong()).emit(env, dest, null)

fun Value.emitStaticMultiply(env: X86Env, by: ULong, dest: Owner) =
    Multiply(this, by.toLong()).emit(env, dest.storage, dest)