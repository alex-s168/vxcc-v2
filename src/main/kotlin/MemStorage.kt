package vxcc

interface MemStorage: Storage {
    fun offsetBytes(offset: Int): MemStorage
}