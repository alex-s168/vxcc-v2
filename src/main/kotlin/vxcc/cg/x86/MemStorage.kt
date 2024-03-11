package vxcc.cg.x86

interface MemStorage: Storage {
    fun offsetBytes(offset: Int): MemStorage
}