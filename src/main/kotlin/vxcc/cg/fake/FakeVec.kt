package vxcc.cg.fake

import vxcc.cg.*

open class FakeVec<E: Env<E>> private constructor(
    val elements: List<Owner<E>>,
    val elemWidth: Int,
): AbstractVectorValue<E>, Storage<E> {

    fun dealloc(env: Env<E>) =
        elements.forEach { env.dealloc(it) }

    companion object {
        fun <E: Env<E>> create(env: E, flags: Owner.Flags): FakeVec<E> {
            val elements = mutableListOf<Owner<E>>()
            val type = when (flags.type) {
                Type.VxUINT -> Type.UINT
                Type.VxINT -> Type.INT
                Type.VxFLT -> Type.FLT
                else -> Type.UINT
            }
            val elemFlags =
                if (flags.use == Env.Use.VECTOR_ARITHM)
                    Owner.Flags(Env.Use.SCALAR_AIRTHM, flags.vecElementWidth!!, null, type)
                else
                    Owner.Flags(Env.Use.STORE, flags.vecElementWidth!!, null, type)

            repeat(flags.totalWidth / flags.vecElementWidth) {
                elements += env.alloc(elemFlags)
            }

            return FakeVec(elements, flags.vecElementWidth)
        }
    }

    override fun emitMov(env: E, dest: Storage<E>) =
        when (dest) {
            is FakeVec<*> -> {
                require(dest.elements.size == elements.size)
                elements.zip(dest.elements).forEach {
                    it.first.storage!!.flatten().emitMov(env, it.second.storage!!.flatten() as Storage<E>)
                }
            }
            is MemStorage -> {
                elements.forEachIndexed { index, owner ->
                    owner.storage!!.flatten().emitMov(env, dest.offsetBytes(index * elemWidth / 8))
                }
            }
            else -> throw Exception("Incompatible destination!")
        }

    override fun emitZero(env: E) =
        elements.forEach { it.storage!!.flatten().emitZero(env) }
}