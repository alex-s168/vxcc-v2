package vxcc.cg.utils

import blitz.flatten
import vxcc.cg.*
import kotlin.math.pow

open class FakeBitSlice<E: CGEnv<E>>(
    val parent: Storage<E>,
    val flags: Owner.Flags,
): Storage<E>,
    AbstractScalarValue<E>,
    DefStaticOpImpl<E>,
    DefFunOpImpl<E>,
    DefArrayIndexImpl<E>,
    PullingStorage<E>
{
    val zeroExtended = mutableMapOf<E, Owner<E>>()
    val defer = mutableListOf<() -> Unit>()

    fun compute(env: E): Owner<E> {
        val o = env.alloc(env.nextUpNative(flags))
        val oSto = o.storage!!.flatten()
        parent.emitMov(env, oSto)
        oSto.emitStaticMask(env, ((2.0).pow(flags.totalWidth) - 1).toLong(), oSto)
        o.canBeDepromoted = Owner.Flags(CGEnv.Use.STORE, flags.totalWidth, null, Type.INT)
        return o
    }

    override fun onDestroy(env: E) {
        zeroExtended.remove(env)?.let { env.dealloc(it) }
        defer.forEach { it() }
    }

    override fun reducedStorage(env: E, flags: Owner.Flags): Storage<E> =
        FakeBitSlice(parent, flags)

    override fun emitZero(env: E) {
        val parentFlags = env.flagsOf(parent)
        parent.emitStaticMask(env, (1L shl flags.totalWidth).inv() and (1L shl parentFlags.totalWidth), parent)
        this.onDestroy(env)
    }

    override fun emitMov(env: E, dest: Storage<E>) =
        when (dest) {
            is FakeBitSlice -> {
                require(dest.flags.totalWidth == this.flags.totalWidth)
                dest.onDestroy(env)
                parent.emitMov(env, dest.parent)
            }
            else -> {
                val zext = zeroExtended.computeIfAbsent(env, ::compute)
                zext.storage!!.flatten().emitMov(env, dest)
            }
        }

    override fun emitPullFrom(env: E, from: Value<E>) =
        when (from) {
            is FakeBitSlice -> from.emitMov(env, this)
            else -> {
                val zext = zeroExtended.computeIfAbsent(env, ::compute)
                from.emitMov(env, zext.storage!!.flatten())
            }
        }

    override fun <V : Value<E>> emitMask(env: E, mask: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitMask(env, mask, dest)
    }

    override fun reduced(env: E, new: Owner.Flags): Value<E> =
        reducedStorage(env, new)

    override fun <V : Value<E>> emitAdd(env: E, other: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitAdd(env, other, dest)
    }

    override fun <V : Value<E>> emitMul(env: E, other: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitMul(env, other, dest)
    }

    override fun <V : Value<E>> emitSignedMul(env: E, other: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitSignedMul(env, other, dest)
    }

    override fun <V : Value<E>> emitShiftLeft(env: E, other: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitShiftLeft(env, other, dest)
    }

    override fun <V : Value<E>> emitShiftRight(env: E, other: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitShiftRight(env, other, dest)
    }

    override fun <V : Value<E>> emitExclusiveOr(env: E, other: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitExclusiveOr(env, other, dest)
    }

    override fun <V : Value<E>> emitSub(env: E, other: V, dest: Storage<E>) {
        val zext = zeroExtended.computeIfAbsent(env, ::compute)
        zext.storage!!.flatten().emitSub(env, other, dest)
    }
}