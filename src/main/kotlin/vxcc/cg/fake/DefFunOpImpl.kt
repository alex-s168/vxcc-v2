package vxcc.cg.fake

import vxcc.cg.Env
import vxcc.cg.Storage
import vxcc.cg.Value

interface DefFunOpImpl<E: Env<E>>: Value<E> {
    override fun <V : Value<E>> emitSignedMax(env: E, other: V, dest: Storage<E>) {
        val thisGr = env.newLocalLabel()
        env.emitJumpIfSignedGreater(this, other, thisGr)
        other.emitMov(env, dest)
        env.switch(thisGr)
        this.emitMov(env, dest)
    }
}