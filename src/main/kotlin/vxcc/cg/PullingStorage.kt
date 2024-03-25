package vxcc.cg

interface PullingStorage<E: CGEnv<E>>: Storage<E> {
    fun emitPullFrom(env: E, from: Value<E>)
}