package vxcc.cg.fake

import vxcc.cg.Env
import vxcc.cg.Storage
import vxcc.cg.Value

interface DefStaticOpImpl<E: Env<E>>: Value<E>, DefStaticLogicOpImpl<E> {
    override fun emitStaticMul(env: E, by: ULong, dest: Storage<E>) =
        emitMul(env, env.immediate(by.toLong(), env.optimal.int.totalWidth), dest)
}