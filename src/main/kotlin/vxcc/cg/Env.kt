package vxcc.cg

interface Env<T: Env<T>> {
    fun forceAllocReg(flags: Owner.Flags, name: String): Owner<T>
    fun alloc(flags: Owner.Flags): Owner<T>
    fun dealloc(owner: Owner<T>)
    fun staticAlloc(widthBytes: Int, init: ByteArray?): MemStorage<T>

    fun makeRegSize(size: Int): Int

    fun immediate(value: Long, width: Int): Value<T>
    fun immediate(value: Double): Value<T>
    fun immediate(value: Float): Value<T>

    fun makeVecFloat(spFloat: Value<T>, count: Int): Owner<T>
    fun makeVecDouble(dpFloat: Value<T>, count: Int): Owner<T>

    val optimal: Optimal<T>

    var optMode: OptMode

    enum class Use {
        STORE,
        SCALAR_AIRTHM,
        VECTOR_ARITHM,
    }

    enum class OptMode {
        SPEED,
        SIZE,
    }
}