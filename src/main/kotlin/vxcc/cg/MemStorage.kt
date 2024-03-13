package vxcc.cg

interface MemStorage<E: Env<E>>: Storage<E> {
    fun offsetBytes(offset: Int): MemStorage<E>
}