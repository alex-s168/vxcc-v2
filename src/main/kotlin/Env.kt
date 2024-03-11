package vxcc

data class Env(
    val target: Target
) {
    fun emit(a: String) =
        println(a)

    fun emitBytes(bytes: ByteArray) =
        emit("db ${bytes.joinToString { "0x${it.toString(16)}" }}")

    val registers = mutableMapOf<Reg.Index, Obj<Owner?>>()

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

    enum class OptMode {
        SPEED,
        SIZE,
    }

    var verboseAsm = false
    var regAlloc = true
    var optMode = OptMode.SPEED

    val optimal = object {
        /** overall fastest boolean type */
        val boolFast = Owner.Flags(Use.SCALAR_AIRTHM, if (target.is32) 32 else 16, null, Type.UINT)

        /** overall smallest boolean type */
        val boolSmall = boolFast

        /** fastest boolean type if speed opt, otherwise smallest boolean type */
        val bool = if (optMode == OptMode.SPEED) boolFast else boolSmall
    }

    internal var fpuUse: Boolean = false

    internal var mmxUse: Boolean = false
        set(value) {
            field = value
            if (!value)
                emit("emms")
        }

    enum class Use {
        STORE,
        SCALAR_AIRTHM,
        VECTOR_ARITHM,
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
            owner.storage.emitMov(this, new.storage)
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
        val gp = flags.use in arrayOf(Use.SCALAR_AIRTHM, Use.STORE) && !flags.type.float && !flags.type.vector

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

        if (flags.use in arrayOf(Use.VECTOR_ARITHM, Use.STORE)) {
            if (target.mmx && flags.totalWidth <= 64 &&
                (flags.use == Use.STORE || flags.vecElementWidth!! in arrayOf(8, 16, 32)) &&
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
                (flags.use == Use.STORE || flags.vecElementWidth!! in arrayOf(8, 16, 32, 64)) &&
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

    fun allocReg(reg: BestRegResult, flags: Owner.Flags): Owner? =
        if (reg.used) null
        else Reg
            .from(reg.index, flags.totalWidth)
            .reducedStorage(this, flags.totalWidth)
            .let { r ->
                val o = Owner(r, flags)
                registers[reg.index] = Obj(o)
                o
            }

    private fun forceAllocReg(reg: BestRegResult, flags: Owner.Flags): Owner {
        if (!reg.used) {
            val r = Reg.from(reg.index, flags.totalWidth)
                .reducedStorage(this, flags.totalWidth)
            val o = Owner(r, flags)
            registers[reg.index] = Obj(o)
            return o
        }

        val owner = registers[reg.index]!!.v!!
        val temp = alloc(owner.flags)
        owner.storage.emitMov(this, temp.storage)
        val new = owner.copy()
        owner.storage = temp.storage
        registers[reg.index] = Obj(new)
        new.storage = new.storage.reducedStorage(this, flags.totalWidth)
        return new
    }

    fun forceAllocReg(flags: Owner.Flags, reg: Reg.Index): Owner =
        forceAllocReg(getRegByIndex(reg), flags)

    fun forceAllocReg(flags: Owner.Flags, name: String): Owner =
        forceAllocReg(flags, Reg.fromName(name).asIndex())

    fun forceAllocRegRecommend(flags: Owner.Flags, recommend: Reg.Index): Owner =
        allocReg(getRegByIndex(recommend), flags) ?: forceAllocReg(flags)

    fun forceAllocRegRecommend(flags: Owner.Flags, recommend: String): Owner =
        forceAllocRegRecommend(flags, Reg.fromName(recommend).asIndex())

    fun allocReg(flags: Owner.Flags): Owner? =
        getBestAvailableReg(flags)?.let { allocReg(it, flags) }

    fun forceAllocReg(flags: Owner.Flags): Owner =
        getBestAvailableReg(flags)?.let { forceAllocReg(it, flags) }
            ?: throw Exception("No compatible register for $flags")

    private val stackAlloc = object {
        inner class Elem(
            val deallocateable: Boolean,
            val byteSize: Int,
        )

        inner class Frame(
            val elements: MutableList<Elem>,
            var nextArr: Long
        )

        val frames = mutableListOf<Frame>()
        val sp = 0L
    }

    fun alloc(flags: Owner.Flags): Owner {
        if (regAlloc) {
            val reg = allocReg(flags)
            if (reg != null)
                return reg
        }

        TODO("implement stack alloc and static alloc")
    }

    fun dealloc(owner: Owner) =
        when (owner.storage) {
            is Reg -> {
                val reg = owner.storage.asReg()
                val id = reg.asIndex()
                reg.onDealloc(owner)
                if (registers.getOrElse(id) { throw Exception("Attempting to deallocate non-existent register!") }.v == null)
                    throw Exception("Attempting to deallocate register twice! Double allocated?")
                if (id.type == Reg.Type.MM)
                    mmxUse = false
                registers[id] = Obj(null)
            }

            else -> TODO("dealloc")
        }

    fun makeRegSize(size: Int): Int =
        if (size <= 8) 8
        else if (size <= 16) 16
        else if (size <= 32) 32
        else if (size <= 64) 64
        else if (size <= 128) 128
        else if (size <= 256) 256
        else if (size <= 512) 512
        else size

    fun immediate(value: Long, width: Int): Immediate =
        Immediate(value, width)

    fun immediate(value: Double, width: Int): Immediate =
        immediate(value.toRawBits(), width)

    fun staticAlloc(widthBytes: Int, init: ByteArray?): MemStorage {
        val arr = init ?: ByteArray(widthBytes)
        require(arr.size == widthBytes)
        TODO()
    }
}