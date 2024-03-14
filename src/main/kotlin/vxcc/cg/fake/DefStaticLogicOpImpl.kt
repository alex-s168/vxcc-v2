package vxcc.cg.fake

import vxcc.cg.Env
import vxcc.cg.Storage
import vxcc.cg.Value

interface DefStaticLogicOpImpl<E: Env<E>>: Value<E> {
    override fun emitStaticMask(env: E, mask: Long, dest: Storage<E>) =
        emitMask(env, env.immediate(mask, env.optimal.int.totalWidth), dest)

    override fun emitStaticShiftLeft(env: E, by: Long, dest: Storage<E>) =
        emitShiftLeft(env, env.immediate(by, env.optimal.int.totalWidth), dest)

    override fun emitStaticShiftRight(env: E, by: Long, dest: Storage<E>) =
        emitShiftRight(env, env.immediate(by, env.optimal.int.totalWidth), dest)
}