package vxcc.cg.x86

import vxcc.cg.Storage

interface MemStorage: Storage {
    fun offsetBytes(offset: Int): MemStorage
}