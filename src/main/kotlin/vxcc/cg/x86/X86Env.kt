package vxcc.cg.x86

import vxcc.arch.x86.X86Target
import vxcc.cg.*
import vxcc.cg.utils.DefMemOpImpl
import vxcc.cg.utils.FakeBitSlice
import vxcc.cg.utils.FakeVec
import blitz.Either
import blitz.Obj
import blitz.flatten
import kotlin.math.ceil

data class X86Env(
    val target: X86Target
): DefMemOpImpl<X86Env> {
    override val source = StringBuilder()

    private val lazyBitWidth by lazy {
        if (target.amd64_v1)
            source.append("bits 64\n")
        else if (target.ia32)
            source.append("bits 32\n")
        else
            source.append("bits 16\n")
    }

    fun emit(a: String) {
        lazyBitWidth
        source.append(a)
        source.append('\n')
    }

    fun emitBytes(bytes: ByteArray) =
        emit("  db ${bytes.joinToString { "0x${it.toString(16)}" }}")

    private val registers = mutableMapOf<Reg.Index, Obj<Owner<X86Env>?>>()

    init {
        for (i in 0..5)
            registers[Reg.Index(Reg.Type.GP, i)] = Obj.of(null)

        if (target.amd64_v1) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.GP64EX, i)] = Obj.of(null)

            for (i in 0..7)
                registers[Reg.Index(Reg.Type.XMM64, i)] = Obj.of(null)
        }

        if (target.mmx) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.MM, i)] = Obj.of(null)
        }

        if (target.sse1) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.XMM, i)] = Obj.of(null)
        }

        if (target.avx512f) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.ZMMEX, i)] = Obj.of(null)
        }
    }

    var regAlloc = true
    var stackAlloc = true

    override var optMode = CGEnv.OptMode.SPEED
    override var optLevel = 0.0f

    override val optimal = object: Optimal<X86Env> {
        /** overall fastest boolean type */
        override val boolFast = Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, if (target.ia32) 32 else 16, null, Type.INT)

        /** overall smallest boolean type */
        override val boolSmall = boolFast

        /** fastest boolean type if speed opt, otherwise smallest boolean type */
        override val bool get() = if (optMode == CGEnv.OptMode.SPEED) boolFast else boolSmall

        /** overall fastest int type */
        override val intFast = Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, if (target.ia32) 32 else 16, null, Type.INT)

        /** overall smallest int type */
        override val intSmall = intFast

        /** fastest int type if speed opt, otherwise smallest int type */
        override val int get() = if (optMode == CGEnv.OptMode.SPEED) boolFast else boolSmall

        override val ptr = Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, if (target.ia32) 32 else if (target.amd64_v1) 64 else 16, null, Type.INT)
    }

    private var fpuUse: Boolean = false

    private var mmxUse: Boolean = false
        set(value) {
            if (!value && field)
                emit("  emms")
            field = value
        }

    data class BestRegResult(
        val index: Reg.Index,
        val used: Boolean,
    )

    private fun tryClaim(index: Reg.Index): BestRegResult? {
        val owner = registers.getOrElse(index) {
            error("Register with index $index not supported on target $target!")
        }.v ?: return BestRegResult(index, false)

        if (owner.shouldBeDestroyed) {
            registers[index] = Obj.of(null)
            return BestRegResult(index, false)
        }

        if (owner.canBeDepromoted != null) {
            registers[index] = Obj.of(Owner.temp()) // we don't want alloc() to return the same reg
            val new = alloc(owner.flags)
            owner.storage!!.flatten().emitMov(this, new.storage!!.flatten())
            dealloc(owner)
            owner.storage = new.storage
            registers[index] = Obj.of(null)
            return BestRegResult(index, false)
        }

        return null
    }

    fun getRegByIndex(index: Reg.Index): BestRegResult =
        tryClaim(index) ?: BestRegResult(index, true)

    private fun getBestAvailableReg(flags: Owner.Flags): BestRegResult? {
        val clobRegs = currentABI?.clobRegs?.map { Reg.fromName(it).asIndex() }
        fun canUse(i: Reg.Index) =
            clobRegs == null || i in clobRegs

        val gp = flags.use in arrayOf(CGEnv.Use.SCALAR_AIRTHM, CGEnv.Use.STORE) && !flags.type.float && !flags.type.vector

        var found: Reg.Index? = null

        if (gp && (flags.totalWidth <= 32 || flags.totalWidth <= 64 && target.amd64_v1)) {
            for (i in arrayOf(  // allocating eax and edx last may improve emitSignedMax // TODO: make IR lowerer use eax for variables which are used in max(0, x)
                1, // b
                2, // c
                4, // si
                5, // di
                0, // a
                3, // d
            )) {
                val index = Reg.Index(Reg.Type.GP, i)
                if (!canUse(index))
                    continue
                found = index

                return tryClaim(found) ?: continue
            }
        }

        if (gp && flags.totalWidth <= 64 && target.amd64_v1) {
            for (i in 0..7) {
                val index = Reg.Index(Reg.Type.GP64EX, i)
                if (!canUse(index))
                    continue
                found = index

                return tryClaim(found) ?: continue
            }
        }

        if (flags.use in arrayOf(CGEnv.Use.VECTOR_ARITHM, CGEnv.Use.STORE)) {
            if (target.mmx && flags.totalWidth <= 64 &&
                (flags.use == CGEnv.Use.STORE || flags.vecElementWidth!! in arrayOf(8, 16, 32)) &&
                !fpuUse && flags.type.int) {
                for (i in 0..7) {
                    val index = Reg.Index(Reg.Type.MM, i)
                    if (!canUse(index))
                        continue
                    found = index

                    val reg = tryClaim(found) ?: continue
                    mmxUse = true
                    return reg
                }
            }

            if (target.sse1 &&
                (flags.totalWidth <= 128 || flags.totalWidth <= 256 && target.avx || flags.totalWidth <= 512 && target.avx512f) &&
                (flags.use == CGEnv.Use.STORE || flags.vecElementWidth!! in arrayOf(8, 16, 32, 64)) &&
                (flags.type.float || flags.type.int && target.sse2)) {
                for (i in 0..7) {
                    val index = Reg.Index(Reg.Type.XMM, i)
                    if (!canUse(index))
                        continue
                    found = index

                    val reg = tryClaim(found) ?: continue
                    return reg
                }
                if (target.amd64_v1) {
                    for (i in 0..7) {
                        val index = Reg.Index(Reg.Type.XMM64, i)
                        if (!canUse(index))
                            continue
                        found = index

                        val reg = tryClaim(found) ?: continue
                        return reg
                    }
                }
                if (target.avx512f) {
                    for (i in 0..7) {
                        val index = Reg.Index(Reg.Type.ZMMEX, i)
                        if (!canUse(index))
                            continue
                        found = index

                        val reg = tryClaim(found) ?: continue
                        return reg
                    }
                }
            }
        }

        return found?.let { BestRegResult(it, true) }
    }

    fun allocReg(reg: BestRegResult, flags: Owner.Flags): Owner<X86Env>? =
        if (reg.used) null
        else Reg.from(reg.index, flags.totalWidth)
            .also { it.vecElementWidth = flags.vecElementWidth }
            .reducedStorage(this, flags)
            .let { r ->
                val o = Owner(Either.ofB(r), flags)
                registers[reg.index] = Obj.of(o)
                o
            }

    private fun forceAllocReg(reg: BestRegResult, flags: Owner.Flags): Owner<X86Env> {
        if (!reg.used) {
            val r = Reg.from(reg.index, flags.totalWidth)
                .also { it.vecElementWidth = flags.vecElementWidth }
                .reducedStorage(this, flags)
            val o = Owner(Either.ofB(r), flags)
            registers[reg.index] = Obj.of(o)
            return o
        }

        val old = registers[reg.index]!!.v!!
        registers[reg.index] = Obj.of(Owner.temp())
        val temp = alloc(old.flags)
        old.storage!!.flatten().emitMov(this, temp.storage!!.flatten())
        val new = old.copy()
        new.storage!!.flatten().asReg().vecElementWidth = flags.vecElementWidth
        old.storage = temp.storage
        registers[reg.index] = Obj.of(new)
        new.storage = Either.ofB(new.storage!!.flatten().reducedStorage(this, flags))
        if (old == new)
            error("uh")
        return new
    }

    fun forceAllocReg(flags: Owner.Flags, reg: Reg.Index): Owner<X86Env> =
        forceAllocReg(getRegByIndex(reg), flags)

    override fun forceAllocReg(flags: Owner.Flags, name: String): Owner<X86Env> =
        if (name == "static")
            Owner(Either.ofB(staticAlloc(flags.totalWidth / 8, null, flags)), flags)
        else
            forceAllocReg(flags, Reg.fromName(name).asIndex())

    override fun forceIntoReg(owner: Owner<X86Env>, name: String) {
        val sto = owner.storage!!.flatten()
        if (sto is Reg && sto.name == name)
            return
        val new = forceAllocReg(owner.flags, name)
        sto.emitMov(this, new.storage!!.flatten())
        dealloc(owner)
        owner.storage = new.storage
    }

    fun forceAllocRegRecommend(flags: Owner.Flags, recommend: Reg.Index): Owner<X86Env> =
        allocReg(getRegByIndex(recommend), flags) ?: forceAllocReg(flags)

    fun forceAllocRegRecommend(flags: Owner.Flags, recommend: String): Owner<X86Env> =
        forceAllocRegRecommend(flags, Reg.fromName(recommend).asIndex())

    fun allocReg(flags: Owner.Flags): Owner<X86Env>? =
        getBestAvailableReg(flags)?.let { allocReg(it, flags) }

    fun forceAllocReg(flags: Owner.Flags): Owner<X86Env> =
        getBestAvailableReg(flags)?.let { forceAllocReg(it, flags) }
            ?: error("No compatible register for $flags")

    private val spRegName = if (target.amd64_v1) "rsp" else if (target.ia32) "esp" else "sp"
    private val bpRegName = if (target.amd64_v1) "rbp" else if (target.ia32) "ebp" else "bp"

    private var nextStackPos = 0

    override fun enterFrame() {
        // emit("  push $spRegName")
        // emit("  mov $bpRegName, $spRegName")
    }

    override fun leaveFrame() {
        // emit("  leave")
    }

    fun stackAlloc(flagsIn: Owner.Flags): Owner<X86Env> {
        val flags = flagsIn.copy(totalWidth = (ceil(flagsIn.totalWidth.toDouble() / 16) * 16).toInt())
        val pos = nextStackPos + flags.totalWidth / 8
        nextStackPos += flags.totalWidth / 8
        return Owner(Either.ofB(X86MemStorage("$bpRegName - $pos", 16, flags, bpRegName, -pos)), flags)
    }

    override fun alloc(flags: Owner.Flags): Owner<X86Env> {
        if (!flags.type.vector) {
            val rsize = makeRegSize(flags.totalWidth)
            if (rsize != flags.totalWidth) {
                val a = alloc(flags.copy(totalWidth = rsize))
                val slice = FakeBitSlice(a.storage!!.flatten(), flags)
                slice.defer += { dealloc(a) }
                return Owner(Either.ofB(slice), flags)
            }
        }

        if (regAlloc) {
            val reg = allocReg(flags)
            if (reg != null)
                return reg
        }

        if (flags.type.vector)
            return Owner(Either.ofB(FakeVec.create(this, flags)), flags)

        if (stackAlloc)
            return stackAlloc(flags)

        return Owner(Either.ofB(staticAlloc(flags.totalWidth / 8, null, flags)), flags)
    }

    override fun dealloc(owner: Owner<X86Env>) {
        val ownerSto = owner.storage!!.flatten()
        ownerSto.onDestroy(this)
        when (ownerSto) {
            is Reg -> {
                val reg = ownerSto.asReg()
                val id = reg.asIndex()
                if (registers.getOrElse(id) { error("Attempting to deallocate non-existent register!") }.v == null)
                    error("Attempting to deallocate register $reg twice! Double allocated?")
                if (id.type == Reg.Type.MM)
                    mmxUse = false
                registers[id] = Obj.of(null)
            }
        }
    }

    override fun makeRegSize(size: Int): Int =
        if (size <= 8) 8
        else if (size <= 16) 16
        else if (size <= 32 && target.ia32) 32
        else if (size <= 64 && (target.amd64_v1 || target.mmx)) 64
        else if (size <= 128 && target.sse1) 128
        else if (size <= 256 && target.avx2) 256
        else if (size <= 512 && target.avx512f) 512
        else error("above native register size!")

    override fun nextUpNative(flags: Owner.Flags): Owner.Flags =
        flags.copy(totalWidth = makeRegSize(flags.totalWidth))

    override fun immediate(value: Long, width: Int): Immediate =
        Immediate(value, width)

    override fun immediate(value: Double): Immediate =
        immediate(value.toRawBits(), 64)

    override fun immediate(value: Float): Immediate =
        immediate(value.toRawBits().toLong(), 32)

    // TODO: 16 byte align stack alloc vals (and make sure callconv asserts that sp aligned 16, otherwise align 16)

    private val staticAllocs = mutableListOf<Triple<String, Int, ByteArray>>()

    override fun staticAlloc(widthBytes: Int, init: ByteArray?, flags: Owner.Flags): MemStorage<X86Env> {
        val label = "_d_a_t_a__${staticAllocs.size}"
        staticLabeledData(label, widthBytes, init)
        return X86MemStorage(label, staticAllocs.last().second.toLong(), flags)
    }

    override fun staticLabeledData(name: String, widthBytes: Int, init: ByteArray?) {
        val arr = init ?: ByteArray(widthBytes)
        require(arr.size == widthBytes)
        val align = if (optMode == CGEnv.OptMode.SPEED) {
            if (target.avx512f) 64
            else if (target.avx2) 32
            else if (target.sse1) 16
            else if (target.mmx || target.amd64_v1) 8
            else if (target.ia32) 4
            else 2
        } else 2
        staticAllocs += Triple(name, align, arr)
    }

    override fun finish() {
        emit("section .data")
        staticAllocs.forEach { (label, align, data) ->
            emit("align $align")
            emit("$label:")
            emitBytes(data)
        }
        staticAllocs.clear()
    }

    override fun makeVecFloat(spFloat: Value<X86Env>, count: Int): Owner<X86Env> {
        val reg = forceAllocReg(Owner.Flags(CGEnv.Use.VECTOR_ARITHM, count * 32, 32, Type.VxFLT))
        val regReg = reg.storage!!.flatten().asReg()
        if (target.avx && regReg.totalWidth in arrayOf(128, 256)) { // xmm and ymm
            spFloat.useInGPReg(this) { valReg ->
                require(valReg.totalWidth == 32)
                emit("  vbroadcastss ${regReg.name}, ${valReg.name}")
            }
        } else if ((target.avx512f || target.avx512vl) && regReg.totalWidth == 512) { // zmm
            spFloat.useInGPReg(this) { valReg ->
                require(valReg.totalWidth == 32)
                emit("  vbroadcastss ${regReg.name}, ${valReg.name}")
            }
        } else {
            TODO("makeVecFloat if not certain CPU features")
        }
        return reg
    }

    override fun makeVecDouble(dpFloat: Value<X86Env>, count: Int): Owner<X86Env> {
        val reg = forceAllocReg(Owner.Flags(CGEnv.Use.VECTOR_ARITHM, count * 64, 64, Type.VxFLT))
        val regReg = reg.storage!!.flatten().asReg()
        if (target.avx && regReg.totalWidth == 256) { // ymm
            dpFloat.useInGPReg(this) { valReg ->
                require(valReg.totalWidth == 64)
                emit("  vbroadcastsd ${regReg.name}, ${valReg.name}")
            }
        } else if (target.avx512f && regReg.totalWidth == 512) { // zmm
            dpFloat.useInGPReg(this) { valReg ->
                require(valReg.totalWidth == 64)
                emit("  vbroadcastsd ${regReg.name}, ${valReg.name}")
            }
        } else {
            TODO("makeVecDouble if not certain CPU features")
        }
        return reg
    }

    override fun switch(label: String) {
        // TODO: do properly later when not depend on assembler
        if (!label.startsWith('.'))
            emit("align 16")
        emit("$label:")
    }

    override fun export(label: String) {
        emit("global $label")
    }

    override fun import(label: String) {
        emit("extern $label")
    }

    private var lidCounter = 0

    override fun newLocalLabel(): String {
        val l = lidCounter ++
        return ".l$l"
    }

    override fun addrToMemStorage(addr: ULong, flags: Owner.Flags): MemStorage<X86Env> =
        X86MemStorage(addr.toString(), addr.toLong(), flags)

    /** COMPLETLY TAKES OVER OWNER */
    override fun addrToMemStorage(addr: Owner<X86Env>, flags: Owner.Flags): MemStorage<X86Env> {
        addr.moveIntoReg(this)
        return X86MemStorage(addr.storage!!.flatten().asReg().name, 1, flags).also {
            dealloc(addr)
        }
    }

    override fun <V: Value<X86Env>> flagsOf(value: V): Owner.Flags =
        when (value) {
            is StorageWithOwner<*> -> value.owner.flags
            is Reg -> Owner.Flags(
                if (value.vecElementWidth == null) CGEnv.Use.SCALAR_AIRTHM else CGEnv.Use.VECTOR_ARITHM,
                value.totalWidth,
                value.vecElementWidth,
                if (value.vecElementWidth == null) Type.INT else Type.VxINT,
            )
            is FakeVec<*> -> Owner.Flags(
                CGEnv.Use.VECTOR_ARITHM,
                value.getWidth(),
                value.elemWidth,
                Type.VxINT,
            )
            is FakeBitSlice<*> -> value.flags
            is X86MemStorage -> value.flags
            else -> TODO("flagsOf $value")
        }

    override fun <V : Value<X86Env>> backToImm(value: V): Long {
        require(value is Immediate)
        return value.value
    }

    override fun emitRet() {
        emit("  ret")
    }

    override fun emitCall(fn: String) {
        emit("  call $fn")
    }

    override fun emitJump(block: String) {
        emit("  jmp $block")
    }

    override fun <V : Value<X86Env>> emitCall(fn: V) {
        fn.useInGPReg(this) {
            emit("  call ${it.name}")
        }
    }

    // TODO: vectors
    private fun <V : Value<X86Env>> emitTest(va: V) {
        when (va) {
            is X86MemStorage -> emit("  cmp ${sizeStr(va.flags.totalWidth)} [${va.emit}], 0")
            else -> va.useInGPReg(this) {
                emit("  test ${it.name}, ${it.name}")
            }
        }
    }

    override fun <V : Value<X86Env>> emitJumpIf(bool: V, block: String) {
        emitTest(bool)
        emit("  jnz $block")
    }

    override fun <V : Value<X86Env>> emitJumpIfNot(bool: V, block: String) {
        emitTest(bool)
        emit("  jz $block")
    }

    private fun <A: Value<X86Env>, B: Value<X86Env>> emitCmp(a: A, b: B) {
        when (a) {
            is X86MemStorage -> when (b) {
                is Immediate -> emit("  cmp ${sizeStr(a.flags.totalWidth)} [${a.emit}], ${b.value}")
                else -> b.useInGPReg(this) { br ->
                    emit("  cmp ${sizeStr(a.flags.totalWidth)} [${a.emit}], ${br.name}")
                }
            }
            else -> a.useInGPReg(this) { ar ->
                when (b) {
                    is Immediate -> emit("  cmp ${ar.name}, ${b.value}")
                    else -> b.useInGPReg(this) { br ->
                        emit("  cmp ${ar.name}, ${br.name}")
                    }
                }
            }
        }
    }

    override fun <A: Value<X86Env>, B: Value<X86Env>> emitJumpIfEq(a: A, b: B, block: String) {
        emitCmp(a, b)
        emit("  je $block")
    }

    override fun <A: Value<X86Env>, B: Value<X86Env>> emitJumpIfNotEq(a: A, b: B, block: String) {
        emitCmp(a, b)
        emit("  jne $block")
    }

    override fun <A: Value<X86Env>, B: Value<X86Env>> emitJumpIfLess(a: A, b: B, block: String) {
        emitCmp(a, b)
        emit("  jb $block")
    }

    override fun <A: Value<X86Env>, B: Value<X86Env>> emitJumpIfGreater(a: A, b: B, block: String) {
        emitCmp(a, b)
        emit("  ja $block")
    }

    override fun <A: Value<X86Env>, B: Value<X86Env>> emitJumpIfSignedLess(a: A, b: B, block: String) {
        emitCmp(a, b)
        emit("  jl $block")
    }

    override fun <A: Value<X86Env>, B: Value<X86Env>> emitJumpIfSignedGreater(a: A, b: B, block: String) {
        emitCmp(a, b)
        emit("  jg $block")
    }

    override fun inlineAsm(inst: String, vararg code: Either<String, Pair<String, Owner<X86Env>>>) {
        val str = StringBuilder()
        str.append("  ")
        str.append(inst)
        str.append(' ')
        code.forEach { e ->
            e.mapA {
                str.append(it)
                str.append(' ')
            }.mapB { (where, what) ->
                when (where) {
                    "r" -> what.moveIntoReg(this)
                    "rm" -> Unit
                    else -> error("Unknown inline assembly arg destination $where")
                }
                str.append(when (val va = what.storage!!.flatten()) {
                    is X86MemStorage -> "${sizeStr(va.flags.totalWidth)} [${va.emit}]"
                    is Reg -> va.name
                    else -> error("wa")
                })
                str.append(' ')
            }
        }
        emit(str.toString())
    }

    override fun addrOf(label: String, dest: Storage<X86Env>) {
        when (dest) {
            is Reg -> emit("  mov ${dest.name}, $label")
            is X86MemStorage -> emit("  mov ${sizeStr(dest.flags.totalWidth)} [${dest.emit}], $label")
            else -> dest.useInGPRegWriteBack(this, copyInBegin = false) { dr ->
                emit("  mov ${dr.name}, $label")
            }
        }
    }

    override fun addrOfAsMemStorage(label: String, flags: Owner.Flags): MemStorage<X86Env> =
        X86MemStorage(label, if (label.startsWith('.')) 1 else 16, flags)

    override fun memSet(dest: MemStorage<X86Env>, value: Byte, len: Int) {
        if (this.optMode == CGEnv.OptMode.SIZE) {
            TODO()
        } else {
            if (this.target.sse1) {
                TODO()
            } else if (this.target.mmx) {
                // TODO: check align
                val reg = this.forceAllocReg(Owner.Flags(CGEnv.Use.VECTOR_ARITHM, 64, 8, Type.VxINT))
                val regSto = reg.storage!!.flatten()
                if (value.toInt() == 0) {
                    regSto.emitZero(this)
                } else {
                    val valueLoc = this.staticAlloc(8, ByteArray(8) { value }, Owner.Flags(CGEnv.Use.STORE, 64, null, Type.INT))
                    valueLoc.emitMov(this, regSto)
                }
                val first = len / 8
                for (i in 0.. first) {
                    val off = i * 8
                    regSto.emitMov(this, dest.offsetBytes(off).reducedStorage(this, Owner.Flags(CGEnv.Use.STORE, 64, null, Type.INT)))
                }
                this.dealloc(reg)
                var left = len % 8
                val valuex4 = value.toLong() or
                        (value.toLong() shl 8) or
                        (value.toLong() shl 16) or
                        (value.toLong() shl 24)
                for (i in 0..left / 4) {
                    this.immediate(valuex4, 32)
                        .emitMov(this, dest.offsetBytes(first + i * 4).reducedStorage(this, Owner.Flags(CGEnv.Use.SCALAR_AIRTHM, 32, null, Type.INT)))
                }
                left %= 4
                if (left > 0) {
                    TODO("unpadded memset (too lazy)")
                }
            } else {
                TODO()
                // unrolled size optimized
            }
        }
    }

    override fun comment(comment: String) {
        emit("; $comment")
    }

    override var currentABI: ABI? = null

    override fun endFnGen() {
        registers.replaceAll { _, _ -> Obj.of(null) }
    }

    override fun storeReg(reg: String, dest: Storage<X86Env>) {
        if (dest is Reg && dest.name == reg)
            return
        Reg.fromName(reg).emitMov(this, dest)
    }

    override fun ownerOf(storage: Storage<X86Env>): Owner<X86Env> {
        registers.values.firstOrNull {
            it.v?.storage?.flatten() == storage
        }?.let {
            return it.v!!
        }

        // TODO: we shouldn't forget static allocs

        return Owner(Either.ofB(storage), flagsOf(storage))
    }

    override fun deallocReg(name: String) {
        registers[Reg.fromName(name).asIndex()] = Obj.of(null)
    }
}
