package vxcc.cg

interface Storage<E: Env<E>>: Value<E> {
    /**
     * Returns a storage object that maps to the lower x bits of the storage.
     * x can not be any value.
     * returned value only exists as long as parent.
     */
    fun reducedStorage(env: E, to: Int): Storage<E>

    /**
     * Zeros out the storage.
     */
    fun emitZero(env: E)
}