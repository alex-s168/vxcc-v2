package vxcc

fun memCpy(env: Env, src: MemStorage, dest: MemStorage, len: Int) {
    TODO("memcpy()")
}

fun memSet(env: Env, dest: MemStorage, value: Byte, len: Int) {
    if (env.optMode == Env.OptMode.SIZE) {

    } else {
        if (env.target.sse1) {
            TODO()
        } else if (env.target.mmx) {
            // TODO: check align
            val reg = env.forceAllocReg(Owner.Flags(Env.Use.VECTOR_ARITHM, 64, 8, Type.VxUINT))
            if (value.toInt() == 0) {
                reg.storage.emitZero(env)
            } else {
                val valueLoc = env.staticAlloc(len, ByteArray(len) { value })
                valueLoc.emitMov(env, reg.storage)
            }
            val first = len / 8
            for (i in 0.. first) {
                val off = i * 8
                reg.storage.emitMov(env, dest.offsetBytes(off))
            }
            env.dealloc(reg)
            var left = len % 8
            for (i in 0..left / 4) {
                env.immediate(value.toLong()).emitMov(env, dest.offsetBytes(first + i * 4).reducedStorage(env, 4))
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

fun memSet(env: Env, dest: MemStorage, value: Value, len: Int) {
    TODO("memset(Value)")
}