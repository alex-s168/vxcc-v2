package vxcc.cg

interface Env<T: Env<T>> {
    fun emitRet()
    fun emitCall(fn: String)
    fun <V: Value<T>> emitCall(fn: V)
    fun emitJump(block: String)
    fun <V: Value<T>> emitJumpIf(bool: V, block: String)
    fun <V: Value<T>> emitJumpIfNot(bool: V, block: String)

    fun forceIntoReg(owner: Owner<T>, name: String)
    fun forceAllocReg(flags: Owner.Flags, name: String): Owner<T>
    fun alloc(flags: Owner.Flags): Owner<T>
    fun dealloc(owner: Owner<T>)
    fun staticAlloc(widthBytes: Int, init: ByteArray?): MemStorage<T>

    fun makeRegSize(size: Int): Int
    fun nextUpNative(flags: Owner.Flags): Owner.Flags

    fun immediate(value: Long, width: Int): Value<T>
    fun immediate(value: Double): Value<T>
    fun immediate(value: Float): Value<T>

    fun makeVecFloat(spFloat: Value<T>, count: Int): Owner<T>
    fun makeVecDouble(dpFloat: Value<T>, count: Int): Owner<T>

    fun newLocalLabel(): String
    fun switch(label: String)
    fun export(label: String)
    fun import(label: String)

    fun addrToMemStorage(addr: ULong, flags: Owner.Flags): MemStorage<T>
    fun <V: Value<T>> addrToMemStorage(addr: V, flags: Owner.Flags): MemStorage<T>

    fun <V: Value<T>> flagsOf(value: V): Owner.Flags

    fun <V: Value<T>> backToImm(value: V): Long

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