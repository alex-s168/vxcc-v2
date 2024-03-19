package vxcc.cg.fake

import vxcc.cg.*

interface DefMemOpImpl<T: Env<T>>: Env<T> {
    override fun memCpy(src: MemStorage<T>, dest: MemStorage<T>, len: Int) {
        if (optMode == Env.OptMode.SPEED) { // TODO: maybe not unroll the whole loop...
            val flags = Owner.Flags(Env.Use.STORE, 8, null, Type.INT)
            repeat(len) { i ->
                src.offsetBytes(i).reduced(this as T, flags).emitMov(this as T, dest.offsetBytes(i).reducedStorage(this as T, flags))
            }
        } else {
            if (len == 0)
                return

            val counter = allocHeated(optimal.int)
            val counterSto = counter.storage!!.flatten()
            counterSto.emitZero(this as T)

            val loop = newLocalLabel()
            val end = newLocalLabel()

            switch(loop)
            val temp = alloc(optimal.int)
            val tempSto = temp.storage!!.flatten()
            dest.emitArrayOffset(this as T, counterSto, (optimal.int.totalWidth / 8).toLong(), tempSto)
            val d = addrToMemStorage(temp, Owner.Flags(Env.Use.STORE, 8, null, Type.INT))
            src.emitArrayIndex(this as T, counterSto, (optimal.int.totalWidth / 8).toLong(), d)

            counterSto.emitStaticAdd(this as T, 1uL, counterSto)
            emitJumpIfLess(counterSto, immediate(len.toLong(), optimal.int.totalWidth), loop)

            switch(end)

            dealloc(counter)
        }
    }

    override fun <V: Value<T>> memCpy(src: MemStorage<T>, dest: MemStorage<T>, len: V) {
        val loop = newLocalLabel()
        val end = newLocalLabel()
        
        emitJumpIfNot(len, end)

        val counter = allocHeated(optimal.int)
        val counterSto = counter.storage!!.flatten()
        counterSto.emitZero(this as T)

        switch(loop)
        val temp = alloc(optimal.int)
        val tempSto = temp.storage!!.flatten()
        dest.emitArrayOffset(this as T, counterSto, (optimal.int.totalWidth / 8).toLong(), tempSto)
        val d = addrToMemStorage(temp, Owner.Flags(Env.Use.STORE, 8, null, Type.INT))
        src.emitArrayIndex(this as T, counterSto, (optimal.int.totalWidth / 8).toLong(), d)

        counterSto.emitStaticAdd(this as T, 1uL, counterSto)
        emitJumpIfLess(counterSto, len, loop)

        switch(end)

        dealloc(counter)
    }

    override fun memSet(dest: MemStorage<T>, value: Byte, len: Int) =
        memSet(dest, immediate(value.toLong(), 8), len)

    override fun <V: Value<T>> memSet(dest: MemStorage<T>, value: Byte, len: V) {
        TODO()
    }

    override fun <V: Value<T>> memSet(dest: MemStorage<T>, value: V, len: Int) {
        TODO()
        if (optMode == Env.OptMode.SPEED) { // TODO: maybe not unroll the whole loop...
            val flags = Owner.Flags(Env.Use.STORE, 8, null, Type.INT)
            repeat(len) { i ->
                value.emitMov(this as T, dest.offsetBytes(i).reducedStorage(this as T, flags))
            }
        } else {
            if (len == 0)
                return

            val counter = allocHeated(optimal.int)
            val counterSto = counter.storage!!.flatten()
            counterSto.emitZero(this as T)

            val loop = newLocalLabel()
            val end = newLocalLabel()

            switch(loop)
            val temp = alloc(optimal.int)
            val tempSto = temp.storage!!.flatten()
            dest.emitArrayOffset(this as T, counterSto, (optimal.int.totalWidth / 8).toLong(), tempSto)
            val d = addrToMemStorage(temp, Owner.Flags(Env.Use.STORE, 8, null, Type.INT))
            value.emitMov(this as T, d)

            counterSto.emitStaticAdd(this as T, 1uL, counterSto)
            emitJumpIfLess(counterSto, immediate(len.toLong(), optimal.int.totalWidth), loop)

            switch(end)

            dealloc(counter)
        }
    }

    override fun <A: Value<T>, B: Value<T>> memSet(dest: MemStorage<T>, value: A, len: B) {
        TODO()
    }
}