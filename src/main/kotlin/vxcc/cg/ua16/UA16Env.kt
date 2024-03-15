package vxcc.cg.ua16

import vxcc.asm.assemble
import vxcc.asm.ua16.UA16Assembler
import vxcc.cg.*
import vxcc.cg.fake.DefMemOpImpl

class UA16Env(
    orig: Int
): DefMemOpImpl<UA16Env> {
    val assembler = UA16Assembler(orig)

    fun emit(asm: String) =
        assemble(asm, assembler)

    fun emitBytes(byteArray: ByteArray) =
        assembler.data(byteArray, mapOf())

    /** a register that will never be allocated */
    val clobReg = "r2"

    var carryGuaranteedUnset = false

    fun unsetCarry() {
        if (!carryGuaranteedUnset)
            emit("clc")
    }

    override fun forceAllocReg(flags: Owner.Flags, name: String): Owner<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun alloc(flags: Owner.Flags): Owner<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun staticAlloc(widthBytes: Int, init: ByteArray?, flags: Owner.Flags): MemStorage<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun staticLabeledData(name: String, widthBytes: Int, init: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun makeRegSize(size: Int): Int {
        TODO("Not yet implemented")
    }

    override fun nextUpNative(flags: Owner.Flags): Owner.Flags {
        TODO("Not yet implemented")
    }

    override fun immediate(value: Long, width: Int): Value<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun immediate(value: Double): Value<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun immediate(value: Float): Value<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun newLocalLabel(): String {
        TODO("Not yet implemented")
    }

    override fun switch(label: String) {
        TODO("Not yet implemented")
    }

    override fun export(label: String) {
        TODO("Not yet implemented")
    }

    override fun import(label: String) {
        TODO("Not yet implemented")
    }

    override fun addrOfAsMemStorage(label: String, flags: Owner.Flags): MemStorage<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun addrToMemStorage(addr: ULong, flags: Owner.Flags): MemStorage<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun enterFrame() {
        TODO("Not yet implemented")
    }

    override fun leaveFrame() {
        TODO("Not yet implemented")
    }

    override fun comment(comment: String) {
        TODO("Not yet implemented")
    }

    override fun finish() {
        TODO("Not yet implemented")
    }

    override val optimal: Optimal<UA16Env>
        get() = TODO("Not yet implemented")
    override var optMode: Env.OptMode
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun inlineAsm(inst: String, vararg code: Either<String, Pair<String, Owner<UA16Env>>>) {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> backToImm(value: V): Long {
        TODO("Not yet implemented")
    }

    override fun <V : Value<UA16Env>> flagsOf(value: V): Owner.Flags {
        TODO("Not yet implemented")
    }

    override fun addrToMemStorage(addr: Owner<UA16Env>, flags: Owner.Flags): MemStorage<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun addrOf(label: String, dest: Storage<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun makeVecDouble(dpFloat: Value<UA16Env>, count: Int): Owner<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun makeVecFloat(spFloat: Value<UA16Env>, count: Int): Owner<UA16Env> {
        TODO("Not yet implemented")
    }

    override fun dealloc(owner: Owner<UA16Env>) {
        TODO("Not yet implemented")
    }

    override fun forceIntoReg(owner: Owner<UA16Env>, name: String) {
        TODO("Not yet implemented")
    }

    override fun emitRet() {
        unsetCarry()
        emit("@retnc $clobReg")
    }

    override fun emitCall(fn: String) {
        unsetCarry()
        emit("@imm $clobReg, $fn")
        emit("@callnc $clobReg")
        carryGuaranteedUnset = false
    }

    override fun emitJump(block: String) {
        unsetCarry()
        emit("@imm $clobReg, $block")
        emit("bnc $clobReg")
        carryGuaranteedUnset = false
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
        a.useInReg { aReg ->
            b.useInReg { bReg ->
                emit("mov $clobReg, $aReg")
                emit("sub $clobReg, $bReg")
                emit("tst $clobReg")
                emit("inv")
                emit("fwc $bReg")

                emit("ltu $aReg, $bReg")
                emit("inv")

                nandCarryWithRegIntoCarry(aReg, bReg)

                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfLess(a: A, b: B, block: String) {
        a.useInReg { aReg ->
            b.useInReg { bReg ->
                emit("ltu $aReg, $bReg")
                emit("inv")
                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfNotEq(a: A, b: B, block: String) {
        a.useInReg { aReg ->
            b.useInReg { bReg ->
                emit("mov $clobReg, $aReg")
                emit("sub $clobReg, $bReg")
                emit("tst $clobReg")
                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <A : Value<UA16Env>, B : Value<UA16Env>> emitJumpIfEq(a: A, b: B, block: String) {
        a.useInReg { aReg ->
            b.useInReg { bReg ->
                emit("mov $clobReg, $aReg")
                emit("sub $clobReg, $bReg")
                emit("tst $clobReg")
                emit("inv")
                emit("@imm $clobReg, $block")
                emit("bnc $clobReg")
            }
        }
    }

    override fun <V : Value<UA16Env>> emitJumpIfNot(bool: V, block: String) {
        bool.useInReg { reg ->
            emit("tst $reg")
            emit("inv")
            emit("@imm $clobReg, $block")
            emit("bnc $clobReg")
        }
    }

    override fun <V : Value<UA16Env>> emitJumpIf(bool: V, block: String) {
        bool.useInReg { reg ->
            emit("tst $reg")
            emit("@imm $clobReg, $block")
            emit("bnc $clobReg")
        }
    }

    override fun <V : Value<UA16Env>> emitCall(fn: V) {
        fn.useInReg { reg ->
            emit("@call $reg")
        }
    }
}