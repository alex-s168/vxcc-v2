package vxcc.cg.x86

import vxcc.cg.Storage

interface X86MemStorage: Storage<X86Env> {
    fun offsetBytes(offset: Int): X86MemStorage
}