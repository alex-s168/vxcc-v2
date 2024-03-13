package vxcc.cg

interface PullingStorage<E: Env<E>>: Storage<E> {
    fun emitPullFrom(env: E, from: Value<E>)
}