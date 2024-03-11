package vxcc.vxcc.x86

interface MemStorage: Storage {
    fun offsetBytes(offset: Int): MemStorage
}