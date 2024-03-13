package vxcc.cg.x86

import vxcc.cg.*

fun X86Env.memCpy(src: MemStorage<X86Env>, dest: MemStorage<X86Env>, len: Int) {
    TODO("memcpy()")
}

fun X86Env.memSet(dest: MemStorage<X86Env>, value: Byte, len: Int) {
    if (this.optMode == Env.OptMode.SIZE) {
        TODO()
    } else {
        if (this.target.sse1) {
            TODO()
        } else if (this.target.mmx) {
            // TODO: check align
            val reg = this.forceAllocReg(Owner.Flags(Env.Use.VECTOR_ARITHM, 64, 8, Type.VxUINT))
            val regSto = reg.storage!!.flatten()
            if (value.toInt() == 0) {
                regSto.emitZero(this)
            } else {
                val valueLoc = this.staticAlloc(8, ByteArray(8) { value })
                valueLoc.emitMov(this, regSto)
            }
            val first = len / 8
            for (i in 0.. first) {
                val off = i * 8
                regSto.emitMov(this, dest.offsetBytes(off))
            }
            this.dealloc(reg)
            var left = len % 8
            val valuex4 = value.toLong() or
                    (value.toLong() shl 8) or
                    (value.toLong() shl 16) or
                    (value.toLong() shl 24)
            for (i in 0..left / 4) {
                this.immediate(valuex4, 32)
                    .emitMov(this, dest.offsetBytes(first + i * 4).reducedStorage(this, Owner.Flags(Env.Use.SCALAR_AIRTHM, 32, null, Type.UINT)))
            }
            left %= 4
            if (left > 0) {
                TODO("unpadded memset (too lazy)")
            }
        } else {
            TODO()
            // unrolled size optimized
        }
    }
}

fun X86Env.memSet(dest: MemStorage<X86Env>, value: Value<X86Env>, len: Int) {
    TODO("memset(Value)")
}