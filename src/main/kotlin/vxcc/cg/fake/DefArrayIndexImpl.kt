package vxcc.cg.fake

import vxcc.cg.*

interface DefArrayIndexImpl<E: Env<E>>: Value<E> {
    override fun <V : Value<E>> emitArrayIndex(env: E, index: V, stride: Long, dest: Storage<E>) {
        val addr = env.alloc(env.optimal.ptr)
        this.emitArrayOffset(env, index, stride, addr.storage!!.flatten())
        val mem = env.addrToMemStorage(addr, env.flagsOf(dest))
        mem.emitMov(env, dest)
    }

    override fun <V : Value<E>> emitArrayOffset(env: E, index: V, stride: Long, dest: Storage<E>) {
        index.emitStaticMul(env, stride.toULong(), dest)
        dest.emitAdd(env, this, dest)
    }
}