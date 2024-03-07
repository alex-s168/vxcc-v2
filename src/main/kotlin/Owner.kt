package vxcc

data class Owner(
    var storage: Storage,
    val flags: Flags,
    var canBeDepromoted: Flags? = null,
    /**
     * This should only be set when there is only one reference to the owner obj
     */
    var shouldBeDestroyed: Boolean = false,
) {
    data class Flags(
        val use: Env.Use,
        val totalWidth: Int,
        val vecElementWidth: Int?,
        val type: Type,
    )

    companion object {
        fun temp(): Owner =
            Owner(Storage.none(), Flags(Env.Use.STORE, 0, null, Type.INT))
    }
}