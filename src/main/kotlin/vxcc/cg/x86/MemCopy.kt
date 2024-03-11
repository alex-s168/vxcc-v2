package vxcc.vxcc.x86

fun memCpy(env: X86Env, src: MemStorage, dest: MemStorage, len: Int) {
    TODO("memcpy()")
}

fun memSet(env: X86Env, dest: MemStorage, value: Byte, len: Int) {
    if (env.optMode == X86Env.OptMode.SIZE) {

    } else {
        if (env.target.sse1) {
            TODO()
        } else if (env.target.mmx) {
            // TODO: check align
            val reg = env.forceAllocReg(Owner.Flags(X86Env.Use.VECTOR_ARITHM, 64, 8, Type.VxUINT))
            if (value.toInt() == 0) {
                reg.storage.emitZero(env)
            } else {
                val valueLoc = env.staticAlloc(8, ByteArray(8) { value })
                valueLoc.emitMov(env, reg.storage)
            }
            val first = len / 8
            for (i in 0.. first) {
                val off = i * 8
                reg.storage.emitMov(env, dest.offsetBytes(off))
            }
            env.dealloc(reg)
            var left = len % 8
            val valuex4 = value.toLong() or
                    (value.toLong() shl 8) or
                    (value.toLong() shl 16) or
                    (value.toLong() shl 24)
            for (i in 0..left / 4) {
                env.immediate(valuex4, 32).emitMov(env, dest.offsetBytes(first + i * 4).reducedStorage(env, 32))
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

fun memSet(env: X86Env, dest: MemStorage, value: Value, len: Int) {
    TODO("memset(Value)")
}