package vxcc

import kotlin.math.ceil

data class Env(
    val target: Target
) {
    fun emit(a: Any) =
        println(a.toString())

    val registers = mutableMapOf<Reg.Index, Owner?>()

    init {
        for (i in 0..5)
            registers[Reg.Index(Reg.Type.GP, i)] = null

        if (target.amd64_v1) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.GP64EX, i)] = null

            for (i in 0..7)
                registers[Reg.Index(Reg.Type.XMM64, i)] = null
        }

        if (target.mmx) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.MM, i)] = null
        }

        if (target.sse1) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.XMM, i)] = null
        }

        if (target.avx512f) {
            for (i in 0..7)
                registers[Reg.Index(Reg.Type.ZMMEX, i)] = null
        }
    }

    var verboseAsm = false

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

    fun getBestAvailableReg(flags: Owner.Flags): BestRegResult? {
        val gp = flags.use in arrayOf(Use.SCALAR_AIRTHM, Use.STORE) && !flags.type.float && !flags.type.vector

        fun tryClaim(index: Reg.Index): BestRegResult? {
            val owner = registers[index]
                ?: return BestRegResult(index, false)

            if (owner.shouldBeDestroyed) {
                registers[index] = null
                return BestRegResult(index, false)
            }

            if (owner.canBeDepromoted != null) {
                registers[index] = Owner.temp() // we don't want alloc() to return the same reg
                val new = alloc(owner.flags)
                owner.storage.emitMov(this, new.storage)
                dealloc(owner)
                owner.storage = new.storage
                registers[index] = null
                return BestRegResult(index, false)
            }

            return null
        }

        var found: Reg.Index? = null

        if (gp && (flags.totalWidth <= 32 || flags.totalWidth <= 64 && target.amd64_v1)) {
            for (i in 0..5) {
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

    fun allocReg(flags: Owner.Flags): Owner? =
        getBestAvailableReg(flags)?.let {
            if (it.used) null
            else Reg
                .from(it.index, flags.totalWidth)
                .let { r ->
                    if (verboseAsm)
                        println("; alloc reg ${r.name}")
                    val o = Owner(r, flags)
                    registers[it.index] = o
                    o
                }
        }

    fun forceAllocReg(flags: Owner.Flags): Owner {
        val reg = getBestAvailableReg(flags)
            ?: throw Exception("No compatible register for $flags")


        if (!reg.used) {
            val r = Reg.from(reg.index, flags.totalWidth)
            if (verboseAsm)
                println("; alloc reg ${r.name}")
            val o = Owner(r, flags)
            registers[reg.index] = o
            return o
        }

        val owner = registers[reg.index]!!
        val temp = alloc(owner.flags)
        owner.storage.emitMov(this, temp.storage)
        val new = owner.copy()
        owner.storage = temp.storage
        registers[reg.index] = new
        if (verboseAsm)
            println("; alloc reg ${new.storage.asReg().name}")
        return new
    }

    fun alloc(flags: Owner.Flags): Owner {
        val reg = allocReg(flags)
        if (reg != null)
            return reg

        TODO("implement stack alloc and static alloc")
    }

    fun dealloc(owner: Owner) =
        when (owner.storage) {
            is Reg -> {
                val reg = owner.storage.asReg()
                val id = reg.asIndex()
                if (verboseAsm)
                    println("; dealloc reg ${reg.name}")
                reg.onDealloc(owner)
                if (registers.getOrElse(id) { throw Exception("Attempting to deallocate non-existent register!") } == null)
                    throw Exception("Attempting to deallocate register twice! Double allocated?")
                if (id.type == Reg.Type.MM)
                    mmxUse = false
                registers[id] = null
            }

            else -> TODO("dealloc")
        }

    fun makeSize(size: Int): Int =
        if (size <= 8) 8
        else if (size <= 16) 16
        else if (size <= 32) 32
        else if (size <= 64) 64
        else if (size <= 128) 128
        else if (size <= 256) 256
        else if (size <= 512) 512
        else size

    fun immediate(value: Long): Immediate =
        Immediate(value)

    fun immediate(value: Double): Immediate =
        immediate(value.toRawBits())
}