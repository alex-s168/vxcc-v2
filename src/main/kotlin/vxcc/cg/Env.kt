package vxcc.cg

import vxcc.vxcc.x86.MemStorage
import vxcc.vxcc.x86.Owner
import vxcc.vxcc.x86.Storage
import vxcc.vxcc.x86.Value

interface Env<T: Env<T>> {
    fun forceAllocReg(flags: Owner.Flags, name: String): Owner
    fun alloc(flags: Owner.Flags): Owner
    fun dealloc(owner: Owner)
    fun staticAlloc(widthBytes: Int, init: ByteArray?): MemStorage

    fun makeRegSize(size: Int): Int

    fun immediate(value: Long, width: Int): Value
    fun immediate(value: Double): Value
    fun immediate(value: Float): Value

    fun makeVecFloat(spFloat: Value, count: Int): Owner
    fun makeVecDouble(dpFloat: Value, count: Int): Owner

    fun shuffleVecX32(vec: Value, vecBitWidth: Int, selection: IntArray, dest: Storage)
    fun shuffleVecX64(vec: Value, vecBitWidth: Int, selection: IntArray, dest: Storage)
    fun shuffleVecX16(vec: Value, vecBitWidth: Int, selection: IntArray, dest: Storage)
    fun shuffleVecX8(vec: Value, vecBitWidth: Int, selection: IntArray, dest: Storage)

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