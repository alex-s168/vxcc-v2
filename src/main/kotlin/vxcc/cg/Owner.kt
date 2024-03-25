package vxcc.cg

import vxcc.utils.Either

data class Owner<E: CGEnv<E>>(
    var storage: Either<StorageWithOwner<E>, Storage<E>>?,
    val flags: Flags,
    var canBeDepromoted: Flags? = null,
    /**
     * This should only be set when there is only one reference to the owner obj
     */
    var shouldBeDestroyed: Boolean = false,
) {
    data class Flags(
        val use: CGEnv.Use,
        val totalWidth: Int,
        val vecElementWidth: Int?,
        val type: Type,
    )

    companion object {
        fun <E: CGEnv<E>> temp(): Owner<E> =
            Owner(null, Flags(CGEnv.Use.STORE, 0, null, Type.INT))
    }
}