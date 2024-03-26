package vxcc.cg

import blitz.Provider
import vxcc.ir.IrGlobalScope
import vxcc.ir.ir
import vxcc.utils.Either
import vxcc.utils.flatten

interface CGEnv<T: CGEnv<T>> {
    val source: StringBuilder
    fun emitRet()
    fun emitCall(fn: String)
    fun <V: Value<T>> emitCall(fn: V)
    fun emitJump(block: String)
    fun <V: Value<T>> emitJumpIf(bool: V, block: String)
    fun <V: Value<T>> emitJumpIfNot(bool: V, block: String)
    fun <A: Value<T>, B: Value<T>> emitJumpIfEq(a: A, b: B, block: String)
    fun <A: Value<T>, B: Value<T>> emitJumpIfNotEq(a: A, b: B, block: String)
    fun <A: Value<T>, B: Value<T>> emitJumpIfLess(a: A, b: B, block: String)
    fun <A: Value<T>, B: Value<T>> emitJumpIfGreater(a: A, b: B, block: String)
    fun <A: Value<T>, B: Value<T>> emitJumpIfSignedLess(a: A, b: B, block: String)
    fun <A: Value<T>, B: Value<T>> emitJumpIfSignedGreater(a: A, b: B, block: String)


    fun ownerOf(storage: Storage<T>): Owner<T>
    fun forceIntoReg(owner: Owner<T>, name: String)
    /** Only returns an owner if new allocated */
    fun forceIntoReg(value: Value<T>, name: String): Owner<T>? {
        if (value is Storage) {
            val owner = ownerOf(value)
            forceIntoReg(owner, name)
            return null
        } else {
            val owner = forceAllocReg(flagsOf(value), name)
            return owner
        }
    }

    /** if `name` is `static` then it needs to be statically allocated instead */
    fun forceAllocReg(flags: Owner.Flags, name: String): Owner<T>
    fun storeReg(reg: String, dest: Storage<T>)
    fun alloc(flags: Owner.Flags): Owner<T>
    fun allocHeated(flags: Owner.Flags): Owner<T> =
        alloc(flags)
    fun dealloc(owner: Owner<T>)
    fun staticAlloc(widthBytes: Int, init: ByteArray?, flags: Owner.Flags): MemStorage<T>
    fun staticLabeledData(name: String, widthBytes: Int, init: ByteArray?)

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
    fun addrOf(label: String, dest: Storage<T>)
    fun addrOfAsMemStorage(label: String, flags: Owner.Flags): MemStorage<T>
    /** should be called after finishing generating a function: resets local allocator */
    fun endFnGen()

    fun addrToMemStorage(addr: ULong, flags: Owner.Flags): MemStorage<T>

    /** COMPLETLY TAKES OVER OWNER */
    fun addrToMemStorage(addr: Owner<T>, flags: Owner.Flags): MemStorage<T>

    fun <V: Value<T>> flagsOf(value: V): Owner.Flags

    fun <V: Value<T>> backToImm(value: V): Long

    fun inlineAsm(inst: String, vararg code: Either<String, Pair<String, Owner<T>>>)

    fun memCpy(src: MemStorage<T>, dest: MemStorage<T>, len: Int)
    fun <V: Value<T>> memCpy(src: MemStorage<T>, dest: MemStorage<T>, len: V)
    fun memSet(dest: MemStorage<T>, value: Byte, len: Int)
    fun <V: Value<T>> memSet(dest: MemStorage<T>, value: Byte, len: V)
    fun <V: Value<T>> memSet(dest: MemStorage<T>, value: V, len: Int)
    fun <A: Value<T>, B: Value<T>> memSet(dest: MemStorage<T>, value: A, len: B)

    fun enterFrame()
    fun leaveFrame()

    fun comment(comment: String)

    fun finish()

    fun ir(lines: Iterator<String>, ctx: IrGlobalScope<T> = IrGlobalScope(), verbose: Boolean = false) {
        ir<T>(lines, this as T, ctx, verbose)
    }

    val optimal: Optimal<T>

    var optMode: OptMode
    /* optimization level from 0 to 1, where 0 is no time-consuming optimizations and 1 is all possible time-consuming optimizations */
    var optLevel: Float

    var currentABI: ABI?

    fun emitAbiCall(
        fn: String,
        abi: ABI,
        args: List<Value<T>> = listOf(),
        retsProvider: Provider<List<Storage<T>>> = { listOf() }
    ) {
        val argOwners = args.zip(abi.argRegs).mapTo(mutableSetOf()) { (v, r) ->
            this.forceIntoReg(v, r)
        }

        val clobs = abi.clobRegs.mapTo(mutableSetOf()) {
            this.forceAllocReg(this.optimal.int, it)
        }

        val oldAbi = this.currentABI
        this.currentABI = abi
        this.emitCall(fn)
        this.currentABI = oldAbi

        argOwners.zip(args).forEach { (o, a) ->
            if (a !is Storage)
                dealloc(o!!)
        }

        clobs.forEach(::dealloc)

        val rets = retsProvider()
        rets.zip(abi.retRegs).forEach { (dest, src) ->
            storeReg(src, dest)
        }
    }

    fun emitAbiCall(
        fn: Value<T>,
        abi: ABI,
        args: List<Value<T>> = listOf(),
        retsProvider: Provider<List<Storage<T>>> = { listOf() }
    ) {
        TODO()
    }

    fun deallocReg(name: String)

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
