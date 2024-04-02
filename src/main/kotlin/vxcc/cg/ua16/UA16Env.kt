package vxcc.cg.ua16

import vxcc.arch.ua16.UA16Target
import vxcc.asm.assemble
import vxcc.asm.ua16.UA16Assembler
import vxcc.cg.*
import vxcc.cg.utils.DefMemOpImpl
import vxcc.cg.utils.FakeBitSlice
import vxcc.cg.utils.FakeVec
import blitz.Either
import blitz.flatten

// TODO: bit slice immediates -> static alloc

class UA16Env(
    orig: Int,
    val target: UA16Target
): DefMemOpImpl<UA16Env> {
    override val source = StringBuilder()
    val assembler = UA16Assembler(orig, target)

    fun emit(asm: String) {
        assemble(asm, assembler)
        source.append("  $asm\n")
    }

    fun emitBytes(byteArray: ByteArray) {
        assembler.data(byteArray, mapOf())
        source.append("  db ${byteArray.joinToString()}\n")
    }

    /** a register that will never be allocated */
    val clobReg = "r2"

    fun unsetCarry() {
        emit("clc")
    }

    val registers = mutableMapOf<String, Owner<UA16Env>?>(
        "r0" to null,
        "r1" to null,
    )

    fun firstFreeReg(): String =
        registers.toList().firstOrNull { it.second == null }?.first ?: error("no free registers")

    override fun forceAllocReg(flags: Owner.Flags, name: String): Owner<UA16Env> {
        if (name == "static") {
            return Owner(Either.ofB(staticAlloc(flags.totalWidth / 8, null, flags)), flags)
        }

        registers[name]?.let { old ->
            if (old.shouldBeDestroyed) {
                old.storage!!.flatten().onDestroy(this)
            } else {
                val new = alloc(old.flags)
                old.storage = new.storage
            }
        }
        val owner = Owner(Either.ofB(UA16Reg(name, flags.totalWidth)), flags)
        registers[name] = owner
        return owner
    }

    // TODO: bit slices
    override fun alloc(flags: Owner.Flags): Owner<UA16Env> =
        Owner(Either.ofB(staticAlloc(flags.totalWidth / 8, null, flags)), flags)

    private val staticAllocs = mutableMapOf<String, ByteArray>()
    private var nextDataId = 0

    override fun staticAlloc(widthBytes: Int, init: ByteArray?, flags: Owner.Flags): MemStorage<UA16Env> {
        val label = "_d_a_t_a__${nextDataId ++}"
        staticLabeledData(label, widthBytes, init)
        return addrOfAsMemStorage(label, flags)
    }

    override fun staticLabeledData(name: String, widthBytes: Int, init: ByteArray?) {
        val arr = init ?: ByteArray(widthBytes)
        require(arr.size == widthBytes)
        staticAllocs[name] = arr
    }

    override fun makeRegSize(size: Int): Int =
        if (size <= 8) 8
        else if (size <= 16) 16
        else error("above native register size!")

    override fun nextUpNative(flags: Owner.Flags): Owner.Flags =
        flags.copy(totalWidth = makeRegSize(flags.totalWidth))

    override fun immediate(value: Long, width: Int): Value<UA16Env> {
        runCatching {
            if (makeRegSize(width) == width)
                return UA16Immediate(value, width)
        }
        val flags = Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, width, null, Type.INT)
        return alloc(flags).storage!!.flatten().also {
            UA16Immediate(value, width).emitMov(this, it)
        }
    }

    override fun immediate(value: Double): Value<UA16Env> =
        error("floats not yet supported on ua16")

    override fun immediate(value: Float): Value<UA16Env> =
        error("floats not yet supported on ua16")

    private var nextLocalLabelId = 0

    override fun newLocalLabel(): String =
        ".l${nextLocalLabelId ++}"

    override fun switch(label: String) {
        source.append("$label:\n")
        assembler.label(label, mapOf())
    }

    override fun export(label: String) {
        // TODO
    }

    override fun import(label: String) {
        // TODO
    }

    override fun addrOfAsMemStorage(label: String, flags: Owner.Flags): MemStorage<UA16Env> =
        UA16MemSto(flags) {
            emit("@imm $it, $label")
        }

    override fun addrToMemStorage(addr: ULong, flags: Owner.Flags): MemStorage<UA16Env> =
        UA16MemSto(flags) {
            immediate(addr.toLong(), optimal.ptr.totalWidth)
                .emitMov(this, UA16Reg(it, optimal.ptr.totalWidth))
        }

    override fun enterFrame() =
        Unit

    override fun leaveFrame() =
        Unit

    override fun comment(comment: String) {
        source.append("; $comment\n")
    }

    override fun finish() {
        staticAllocs.forEach { (k, v) ->
            assembler.label(k, mapOf())
            source.append("$k:")
            emitBytes(v)
        }
        assembler.finish()
    }

    override val optimal = object : Optimal<UA16Env> {
        override val bool = Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, 8, null, Type.INT)
        override val boolFast = bool
        override val boolSmall = bool

        override val int = Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, 8, null, Type.INT)
        override val intFast = int
        override val intSmall = int

        override val ptr = Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, 16, null, Type.INT)
    }

    override var optMode = CGEnv.OptMode.SPEED
    override var optLevel = 0.0f

    override fun inlineAsm(inst: String, vararg code: Either<String, Pair<String, Owner<UA16Env>>>) {
       TODO()
    }

    override fun <V : Value<UA16Env>> backToImm(value: V): Long =
        (value as UA16Immediate).value

    override fun <V : Value<UA16Env>> flagsOf(value: V): Owner.Flags =
        when (value) {
            is FakeBitSlice<*> -> value.flags
            is FakeVec<*> -> Owner.Flags(CGEnv.Use.VECTOR_ARITHM, value.elements.size * value.elemWidth, value.elemWidth, Type.VxINT)
            is UA16Immediate -> Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, value.width, null, Type.INT)
            is UA16Reg -> Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, value.width, null, Type.INT)
            is UA16MemSto -> value.flags
            else -> TODO()
        }

    override fun addrToMemStorage(addr: Owner<UA16Env>, flags: Owner.Flags): MemStorage<UA16Env> =
        UA16MemSto(flags) { r ->
            addr.storage!!.flatten().emitMov(this, UA16Reg(r, optimal.ptr.totalWidth))
        }.also {
            it.defer += { dealloc(addr) }
        }

    override fun addrOf(label: String, dest: Storage<UA16Env>) {
        dest.useInRegWriteBack(this, copyInBegin = false) { reg ->
            emit("@imm $reg, $label")
        }
    }

    override fun makeVecDouble(dpFloat: Value<UA16Env>, count: Int): Owner<UA16Env> =
        error("ua16 currently does not support floats!")

    override fun makeVecFloat(spFloat: Value<UA16Env>, count: Int): Owner<UA16Env> =
        error("ua16 currently does not support floats!")

    override fun dealloc(owner: Owner<UA16Env>) {
        val sto = owner.storage!!.flatten()
        sto.onDestroy(this)
        if (sto is UA16Reg) {
            registers[sto.name] = null
        }
    }

    override fun forceIntoReg(owner: Owner<UA16Env>, name: String) {
        val ownerSto = owner.storage!!.flatten()
        if (ownerSto is UA16Reg && ownerSto.name == name)
            return
        val new = forceAllocReg(owner.flags, name)
        ownerSto.emitMov(this, new.storage!!.flatten())
        dealloc(owner)
        owner.storage = new.storage
    }

    override fun emitRet() {
        unsetCarry()
        emit("@retnc clob=$clobReg")
    }

    override fun emitCall(fn: String) {
        emit("@imm $clobReg, $fn")
        unsetCarry()
        emit("@callnc $clobReg")
    }

    override fun emitJump(block: String) {
        emit("@imm $clobReg, $block")
        unsetCarry()
        emit("bnc $clobReg")
    }

    private fun nandCarryWithRegIntoCarry(clobExtra: String, b: String) {
        emit("fwc $clobExtra")
        // aClob = is zero aClob

        emit("tst $b")
        emit("fwc $clobReg")
        // clobReg = is zero b

        emit("orr $clobReg, $clobExtra")
        // clobReg = (is zero aClob) or (is zero b)

        emit("ec9 $clobReg")
        // carry = (is zero aClob) or (is zero b)
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfSignedGreater(a: A, b: B, block: String) {
        TODO("ua16 signed integers")
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfSignedLess(a: A, b: B, block: String) {
        TODO("ua16 signed integers")
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfGreater(a: A, b: B, block: String) {
        a.useInReg(this) { aReg ->
            b.useInReg(this) { bReg ->
                emit("mov $clobReg, ${aReg.name}")
                emit("sub $clobReg, ${bReg.name}")
                emit("tst $clobReg")
                emit("inv")
                emit("fwc ${bReg.name}")

                emit("ltu ${aReg.name}, ${bReg.name}")
                emit("inv")

                nandCarryWithRegIntoCarry(aReg.name, bReg.name)

                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfLess(a: A, b: B, block: String) {
        a.useInReg(this) { aReg ->
            b.useInReg(this) { bReg ->
                emit("ltu ${aReg.name}, ${bReg.name}")
                emit("inv")
                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfNotEq(a: A, b: B, block: String) {
        a.useInReg(this) { aReg ->
            b.useInReg(this) { bReg ->
                emit("mov $clobReg, ${aReg.name}")
                emit("sub $clobReg, ${bReg.name}")
                emit("tst $clobReg")
                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfEq(a: A, b: B, block: String) {
        a.useInReg(this) { aReg ->
            b.useInReg(this) { bReg ->
                emit("mov $clobReg, ${aReg.name}")
                emit("sub $clobReg, ${bReg.name}")
                emit("tst $clobReg")
                emit("inv")
                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <V : Value<UA16Env>> emitJumpIfNot(bool: V, block: String) {
        bool.useInReg(this) { reg ->
            emit("tst ${reg.name}")
            emit("inv")
            emit("@imm $clobReg, $block")
            emit("bnc $clobReg")
        }
    }

    override fun <V : Value<UA16Env>> emitJumpIf(bool: V, block: String) {
        bool.useInReg(this) { reg ->
            emit("tst ${reg.name}")
            emit("@imm $clobReg, $block")
            emit("bnc $clobReg")
        }
    }

    override fun <V : Value<UA16Env>> emitCall(fn: V) {
        fn.useInReg(this) { reg ->
            emit("@call ${reg.name}")
        }
    }

    override var currentABI: ABI? = null

    override fun endFnGen() {
        registers["r0"] = null
        registers["r1"] = null
    }

    override fun storeReg(reg: String, dest: Storage<UA16Env>) {
        if (dest is UA16Reg && dest.name == reg)
            return
        UA16Reg(reg, flagsOf(dest).totalWidth).emitMov(this, dest)
    }

    override fun ownerOf(storage: Storage<UA16Env>): Owner<UA16Env> {
        registers.values.firstOrNull {
            it?.storage?.flatten() == storage
        }?.let {
            return it
        }

        // TODO: we shouldn't forget static allocs

        return Owner(Either.ofB(storage), flagsOf(storage))
    }

    override fun deallocReg(name: String) {
        registers[name] = null
    }
}
