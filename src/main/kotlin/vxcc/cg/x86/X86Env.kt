package vxcc.cg.x86

import vxcc.cg.*
import vxcc.cg.fake.FakeBitSlice
import vxcc.cg.fake.FakeVec

data class X86Env(
    val target: Target
): Env<X86Env> {
    fun emit(a: String) =
        println(a)

    fun emitBytes(bytes: ByteArray) =
        emit("  db ${bytes.joinToString { "0x${it.toString(16)}" }}")

    val registers = mutableMapOf<Reg.Index, Obj<Owner<X86Env>?>>()

    init {
        for (i in 0..5)
            registers[Reg.Index(Reg.Type.GP, i)] = Obj(null)

        if (target.amd64_v1) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.GP64EX, i)] = Obj(null)

            for (i in 0..7)
                registers[Reg.Index(Reg.Type.XMM64, i)] = Obj(null)
        }

        if (target.mmx) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.MM, i)] = Obj(null)
        }

        if (target.sse1) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.XMM, i)] = Obj(null)
        }

        if (target.avx512f) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.ZMMEX, i)] = Obj(null)
        }
    }

    var regAlloc = true
    override var optMode = Env.OptMode.SPEED

    override val optimal = object: Optimal<X86Env> {
        /** overall fastest boolean type */
        override val boolFast = Owner.Flags(Env.Use.SCALAR_AIRTHM, if (target.is32) 32 else 16, null, Type.UINT)

        /** overall smallest boolean type */
        override val boolSmall = boolFast

        /** fastest boolean type if speed opt, otherwise smallest boolean type */
        override val bool = if (optMode == Env.OptMode.SPEED) boolFast else boolSmall

        /** overall fastest int type */
        override val intFast = Owner.Flags(Env.Use.SCALAR_AIRTHM, if (target.is32) 32 else 16, null, Type.UINT)

        /** overall smallest int type */
        override val intSmall = intFast

        /** fastest int type if speed opt, otherwise smallest int type */
        override val int = if (optMode == Env.OptMode.SPEED) boolFast else boolSmall

        override val ptr = Owner.Flags(Env.Use.SCALAR_AIRTHM, if (target.is32) 32 else if (target.amd64_v1) 64 else 16, null, Type.UINT)
    }

    internal var fpuUse: Boolean = false

    internal var mmxUse: Boolean = false
        set(value) {
            field = value
            if (!value)
                emit("  emms")
        }

    data class BestRegResult(
        val index: Reg.Index,
        val used: Boolean,
    )

    private fun tryClaim(index: Reg.Index): BestRegResult? {
        val owner = registers.getOrElse(index) {
            throw Exception("Register with index $index not supported on target $target!")
        }.v ?: return BestRegResult(index, false)

        if (owner.shouldBeDestroyed) {
            registers[index] = Obj(null)
            return BestRegResult(index, false)
        }

        if (owner.canBeDepromoted != null) {
            registers[index] = Obj(Owner.temp()) // we don't want alloc() to return the same reg
            val new = alloc(owner.flags)
            owner.storage!!.flatten().emitMov(this, new.storage!!.flatten())
            dealloc(owner)
            owner.storage = new.storage
            registers[index] = Obj(null)
            return BestRegResult(index, false)
        }

        return null
    }

    fun getRegByIndex(index: Reg.Index): BestRegResult =
        tryClaim(index) ?: BestRegResult(index, true)

    private fun getBestAvailableReg(flags: Owner.Flags): BestRegResult? {
        val gp = flags.use in arrayOf(Env.Use.SCALAR_AIRTHM, Env.Use.STORE) && !flags.type.float && !flags.type.vector

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
                found = Reg.Index(Reg.Type.GP, i)
                return tryClaim(found) ?: continue
            }
        }

        if (gp && flags.totalWidth <= 64 && target.amd64_v1) {
            for (i in 0..7) {
                found = Reg.Index(Reg.Type.GP64EX, i)
                return tryClaim(found) ?: continue
            }
        }

        if (flags.use in arrayOf(Env.Use.VECTOR_ARITHM, Env.Use.STORE)) {
            if (target.mmx && flags.totalWidth <= 64 &&
                (flags.use == Env.Use.STORE || flags.vecElementWidth!! in arrayOf(8, 16, 32)) &&
                !fpuUse && flags.type.int) {
                for (i in 0..7) {
                    found = Reg.Index(Reg.Type.MM, i)
                    val reg = tryClaim(found) ?: continue
                    mmxUse = true
                    return reg
                }
            }

            if (target.sse1 &&
                (flags.totalWidth <= 128 || flags.totalWidth <= 256 && target.avx || flags.totalWidth <= 512 && target.avx512f) &&
                (flags.use == Env.Use.STORE || flags.vecElementWidth!! in arrayOf(8, 16, 32, 64)) &&
                (flags.type.float || flags.type.int && target.sse2)) {
                for (i in 0..7) {
                    found = Reg.Index(Reg.Type.XMM, i)
                    val reg = tryClaim(found) ?: continue
                    return reg
                }
                if (target.amd64_v1) {
                    for (i in 0..7) {
                        found = Reg.Index(Reg.Type.XMM64, i)
                        val reg = tryClaim(found) ?: continue
                        return reg
                    }
                }
                if (target.avx512f) {
                    for (i in 0..7) {
                        found = Reg.Index(Reg.Type.ZMMEX, i)
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
                registers[reg.index] = Obj(o)
                o
            }

    private fun forceAllocReg(reg: BestRegResult, flags: Owner.Flags): Owner<X86Env> {
        if (!reg.used) {
            val r = Reg.from(reg.index, flags.totalWidth)
                .also { it.vecElementWidth = flags.vecElementWidth }
                .reducedStorage(this, flags)
            val o = Owner(Either.ofB(r), flags)
            registers[reg.index] = Obj(o)
            return o
        }

        val owner = registers[reg.index]!!.v!!
        val temp = alloc(owner.flags)
        owner.storage!!.flatten().emitMov(this, temp.storage!!.flatten())
        val new = owner.copy()
        new.storage!!.flatten().asReg().vecElementWidth = flags.vecElementWidth
        owner.storage = temp.storage
        registers[reg.index] = Obj(new)
        new.storage = Either.ofB(new.storage!!.flatten().reducedStorage(this, flags))
        return new
    }

    fun forceAllocReg(flags: Owner.Flags, reg: Reg.Index): Owner<X86Env> =
        forceAllocReg(getRegByIndex(reg), flags)

    override fun forceAllocReg(flags: Owner.Flags, name: String): Owner<X86Env> =
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
            ?: throw Exception("No compatible register for $flags")

    override fun alloc(flags: Owner.Flags): Owner<X86Env> {
        if (regAlloc) {
            val reg = allocReg(flags)
            if (reg != null)
                return reg
        }

        if (flags.type.vector)
            return Owner(Either.ofB(FakeVec.create(this, flags)), flags)

        TODO("implement stack alloc and static alloc")
    }

    override fun dealloc(owner: Owner<X86Env>) {
        val ownerSto = owner.storage!!.flatten()
        ownerSto.onDestroy(this)
        when (ownerSto) {
            is Reg -> {
                val reg = ownerSto.asReg()
                val id = reg.asIndex()
                if (registers.getOrElse(id) { throw Exception("Attempting to deallocate non-existent register!") }.v == null)
                    throw Exception("Attempting to deallocate register twice! Double allocated?")
                if (id.type == Reg.Type.MM)
                    mmxUse = false
                registers[id] = Obj(null)
            }
        }
    }

    override fun makeRegSize(size: Int): Int =
        if (size <= 8) 8
        else if (size <= 16) 16
        else if (size <= 32) 32
        else if (size <= 64) 64
        else if (size <= 128) 128
        else if (size <= 256) 256
        else if (size <= 512) 512
        else size

    override fun nextUpNative(flags: Owner.Flags): Owner.Flags =
        flags.copy(totalWidth = makeRegSize(flags.totalWidth))

    override fun immediate(value: Long, width: Int): Immediate =
        Immediate(value, width)

    override fun immediate(value: Double): Immediate =
        immediate(value.toRawBits(), 64)

    override fun immediate(value: Float): Immediate =
        immediate(value.toRawBits().toLong(), 32)

    // TODO: 16 byte align stack alloc vals (and make sure callconv asserts that sp aligned 16, otherwise align 16)

    override fun staticAlloc(widthBytes: Int, init: ByteArray?): MemStorage<X86Env> {
        val arr = init ?: ByteArray(widthBytes)
        require(arr.size == widthBytes)
        // if speed then align 16 else align idk
        // ymm wants align 32
        // zmm wants align 64
        TODO()
    }

    override fun makeVecFloat(spFloat: Value<X86Env>, count: Int): Owner<X86Env> {
        val reg = forceAllocReg(Owner.Flags(Env.Use.VECTOR_ARITHM, count * 32, 32, Type.VxFLT))
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
        val reg = forceAllocReg(Owner.Flags(Env.Use.VECTOR_ARITHM, count * 64, 64, Type.VxFLT))
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


    override fun addrToMemStorage(addr: ULong, flags: Owner.Flags): MemStorage<X86Env> {
        TODO()
    }

    override fun <V: Value<X86Env>> addrToMemStorage(addr: V, flags: Owner.Flags): MemStorage<X86Env> {
        TODO()
    }

    override fun <V: Value<X86Env>> flagsOf(value: V): Owner.Flags =
        when (value) {
            is StorageWithOwner<*> -> value.owner.flags
            is Reg -> Owner.Flags(
                if (value.vecElementWidth == null) Env.Use.SCALAR_AIRTHM else Env.Use.VECTOR_ARITHM,
                value.totalWidth,
                value.vecElementWidth,
                if (value.vecElementWidth == null) Type.UINT else Type.VxUINT,
            )
            is FakeVec<*> -> Owner.Flags(
                Env.Use.VECTOR_ARITHM,
                value.getWidth(),
                value.elemWidth,
                Type.VxUINT,
            )
            is FakeBitSlice<*> -> value.flags
            is StackSlot -> Owner.Flags(
                if (value.vecElemWidth == null) Env.Use.SCALAR_AIRTHM else Env.Use.VECTOR_ARITHM,
                value.width,
                value.vecElemWidth,
                if (value.vecElemWidth == null) Type.UINT else Type.VxUINT,
            )
            else -> TODO("flagsOf $value")
        }

    override fun <V : Value<X86Env>> backToImm(value: V): Long {
        require(value is Immediate)
        return value.value
    }
}