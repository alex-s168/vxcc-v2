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
}