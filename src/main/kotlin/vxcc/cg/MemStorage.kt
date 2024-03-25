package vxcc.cg

interface MemStorage<E: CGEnv<E>>: Storage<E> {
    fun offsetBytes(offset: Int): MemStorage<E>
}