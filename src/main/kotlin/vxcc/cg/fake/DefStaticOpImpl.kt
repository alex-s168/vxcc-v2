package vxcc.cg.fake

import vxcc.cg.Env
import vxcc.cg.Storage
import vxcc.cg.Value

interface DefStaticOpImpl<E: Env<E>>: Value<E>, DefStaticLogicOpImpl<E> {
    override fun emitStaticMul(env: E, by: ULong, dest: Storage<E>) =
        emitMul(env, env.immediate(by.toLong(), env.optimal.int.totalWidth), dest)

    override fun emitStaticAdd(env: E, other: ULong, dest: Storage<E>) =
        emitAdd(env, env.immediate(other.toLong(), env.optimal.int.totalWidth), dest)

    override fun emitStaticSub(env: E, other: ULong, dest: Storage<E>) =
        emitSub(env, env.immediate(other.toLong(), env.optimal.int.totalWidth), dest)
}