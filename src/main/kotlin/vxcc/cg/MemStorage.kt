package vxcc.cg

import vxcc.cg.x86.X86Env

interface MemStorage: Storage<X86Env> {
    fun offsetBytes(offset: Int): MemStorage
}