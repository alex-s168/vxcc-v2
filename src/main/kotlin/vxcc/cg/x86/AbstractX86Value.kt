package vxcc.cg.x86

import vxcc.cg.*
import vxcc.cg.utils.DefStaticOpImpl
import vxcc.utils.Either

interface AbstractX86Value: Value<X86Env>, DefStaticOpImpl<X86Env> {
    /** dest = this + index * stride */
    override fun <V: Value<X86Env>> emitArrayOffset(env: X86Env, index: V, stride: Long, dest: Storage<X86Env>) =
        X86ArrIndex(
            if (this is StorageWithOwner<*>) Either.ofA(this.owner as Owner<X86Env>) else Either.ofB(this),
            if (index is StorageWithOwner<*>) Either.ofA(index.owner as Owner<X86Env>) else Either.ofB(index),
            stride
        ).emitOffset(env, dest)

    /** dest = * (typeof(dest) *) (this + index * stride) */
    override fun <V: Value<X86Env>> emitArrayIndex(env: X86Env, index: V, stride: Long, dest: Storage<X86Env>) =
        X86ArrIndex(
            if (this is StorageWithOwner<*>) Either.ofA(this.owner as Owner<X86Env>) else Either.ofB(this),
            if (index is StorageWithOwner<*>) Either.ofA(index.owner as Owner<X86Env>) else Either.ofB(index),
            stride
        ).emitIndex(env, dest)

    override fun emitStaticMul(env: X86Env, by: ULong, dest: Storage<X86Env>) =
        X86Multiply(this, by.toLong()).emit(env, dest, null)
}