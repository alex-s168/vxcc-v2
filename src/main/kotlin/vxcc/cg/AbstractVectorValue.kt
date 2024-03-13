package vxcc.cg

interface AbstractVectorValue<E: Env<E>>: Value<E> {
    override fun <V : Value<E>> emitAdd(env: E, other: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun reduced(env: E, new: Owner.Flags): Value<E> {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitArrayIndex(env: E, index: V, stride: Long, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitArrayOffset(env: E, index: V, stride: Long, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitExclusiveOr(env: E, other: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitMask(env: E, mask: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitMul(env: E, other: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitShiftLeft(env: E, other: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitShiftRight(env: E, other: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitSignedMax(env: E, other: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun <V : Value<E>> emitSignedMul(env: E, other: V, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun emitStaticMask(env: E, mask: Long, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun emitStaticMul(env: E, by: ULong, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun emitStaticShiftLeft(env: E, by: Long, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }

    override fun emitStaticShiftRight(env: E, by: Long, dest: Storage<E>) {
        throw Exception("Can not perform scalar operation on vector value!")
    }
}