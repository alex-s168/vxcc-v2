package vxcc.cg

import vxcc.cg.utils.DefStaticOpImpl

interface AbstractVectorValue<E: CGEnv<E>>: Value<E>, DefStaticOpImpl<E> {
    override fun <V : Value<E>> emitAdd(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitSub(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun reduced(env: E, new: Owner.Flags): Value<E> {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitArrayIndex(env: E, index: V, stride: Long, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitArrayOffset(env: E, index: V, stride: Long, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitExclusiveOr(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitMask(env: E, mask: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitMul(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitShiftLeft(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitShiftRight(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitSignedMax(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitSignedMul(env: E, other: V, dest: Storage<E>) {
        error("Can not perform scalar operation on vector value!")
    }
}