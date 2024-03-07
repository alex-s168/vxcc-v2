package vxcc

import kotlin.math.pow

data class Reg(
    val name: String,
    val totalWidth: Int,
    val type: Type,
    val localId: Int,
): Storage {
    fun asIndex(): Index =
        Index(type, localId)

    data class Index(
        val type: Type,
        val localId: Int,
    )

    enum class Type {
        /** on amd64_v1: 6 64-bit GP registers; else 32-bit;
         * (E/R)SP and (E/R)BP are not included */
        GP,
        /** unlocked by amd64_v1: extra 8 64-bit GP registers */
        GP64EX,
        /** unlocked by mmx: 8 mmx (64-bit vector) registers mapped onto the fpu stack */
        MM,
        /** unlocked by sse1: 8 sse (128-bit vector) registers;
        *  on avx 256-bit vector;
        *  on avx512f 512-bit vector */
        XMM,
        /** unlocked by amd64_v1: extra 8 sse (128-bit vector) register;
        *  on avx 256-bit vector;
        *  on avx512f 512-bit vector */
        XMM64,
        /** unlocked by avx512f: extra 8 avx registers (512-bit vector) */
        ZMMEX,
    }

    companion object {
        fun from(index: Index, size: Int): Reg =
            when (index.type) {
                Type.GP -> getGP(index.localId, size)
                Type.GP64EX -> getGP64EX(index.localId, size)
                Type.MM -> getMM(index.localId, size)
                Type.XMM -> getXMM(index.localId, size)
                Type.XMM64 -> getXMM64(index.localId, size)
                Type.ZMMEX -> getZMMEX(index.localId, size)
            }

        fun getGP(localId: Int, size: Int): Reg =
            when (size) {
                8 -> when (localId) {
                    0 -> Reg("al",  8, Type.GP, localId)
                    1 -> Reg("bl",  8, Type.GP, localId)
                    2 -> Reg("cl",  8, Type.GP, localId)
                    3 -> Reg("dl",  8, Type.GP, localId)
                    4 -> Reg("sil", 8, Type.GP, localId)
                    5 -> Reg("dil", 8, Type.GP, localId)
                    else -> throw Exception("There are only 6 GP registers!")
                }
                16 -> when (localId) {
                    0 -> Reg("ax", 16, Type.GP, localId)
                    1 -> Reg("bx", 16, Type.GP, localId)
                    2 -> Reg("cx", 16, Type.GP, localId)
                    3 -> Reg("dx", 16, Type.GP, localId)
                    4 -> Reg("si", 16, Type.GP, localId)
                    5 -> Reg("di", 16, Type.GP, localId)
                    else -> throw Exception("There are only 6 GP registers!")
                }
                32 -> when (localId) {
                    0 -> Reg("eax", 32, Type.GP, localId)
                    1 -> Reg("ebx", 32, Type.GP, localId)
                    2 -> Reg("ecx", 32, Type.GP, localId)
                    3 -> Reg("edx", 32, Type.GP, localId)
                    4 -> Reg("esi", 32, Type.GP, localId)
                    5 -> Reg("edi", 32, Type.GP, localId)
                    else -> throw Exception("There are only 6 GP registers!")
                }
                64 -> when (localId) {
                    0 -> Reg("rax", 64, Type.GP, localId)
                    1 -> Reg("rbx", 64, Type.GP, localId)
                    2 -> Reg("rcx", 64, Type.GP, localId)
                    3 -> Reg("rdx", 64, Type.GP, localId)
                    4 -> Reg("rsi", 64, Type.GP, localId)
                    5 -> Reg("rdi", 64, Type.GP, localId)
                    else -> throw Exception("There are only 6 GP registers!")
                }
                else -> throw Exception("Invalid GP register size!")
            }

        fun getGP64EX(localId: Int, size: Int): Reg {
            if (localId !in 0..7)
                throw Exception("There are only 8 GP64EX registers!")
            val id = localId + 8
            return when (size) {
                8 -> Reg("r${id}b", 8, Type.GP64EX, localId)
                16 -> Reg("r${id}w", 16, Type.GP64EX, localId)
                32 -> Reg("r${id}d", 32, Type.GP64EX, localId)
                64 -> Reg("rid", 64, Type.GP64EX, localId)
                else -> throw Exception("Invalid GP64EX register size!")
            }
        }

        fun getMM(localId: Int, size: Int): Reg {
            if (localId !in 0..7)
                throw Exception("There are only 8 MM registers!")
            if (size != 64)
                throw Exception("Invalid MM register size!")
            return Reg("mm${localId}", 64, Type.MM, localId)
        }

        fun getXMM(localId: Int, size: Int): Reg {
            if (localId !in 0..7)
                throw Exception("There are only 8 XMM registers!")
            return when (size) {
                128 -> Reg("xmm${localId}", 128, Type.XMM, localId)
                256 -> Reg("ymm${localId}", 256, Type.XMM, localId)
                512 -> Reg("zmm${localId}", 512, Type.XMM, localId)
                else -> throw Exception("Invalid XMM register size!")
            }
        }

        fun getXMM64(localId: Int, size: Int): Reg {
            if (localId !in 0..7)
                throw Exception("There are only 8 XMM64 registers!")
            val id = localId + 8
            return when (size) {
                128 -> Reg("xmm${id}", 128, Type.XMM64, localId)
                256 -> Reg("ymm${id}", 256, Type.XMM64, localId)
                512 -> Reg("zmm${id}", 512, Type.XMM64, localId)
                else -> throw Exception("Invalid XMM64 register size!")
            }
        }

        fun getZMMEX(localId: Int, size: Int): Reg {
            if (localId !in 0..7)
                throw Exception("There are only 8 ZMMEX registers!")
            val id = localId + 16
            if (size != 512)
                throw Exception("Invalid ZMMEX register size!")
            return  Reg("zmm${id}", 512, Type.ZMMEX, localId)
        }
    }

    private val views = mutableListOf<View>()

    data class View internal constructor(
        val reg: Reg,
        val size: Int,
    ): Storage {
        init {
            reg.views.add(this)
        }

        val zextMap = mutableMapOf<Env, Owner>()

        fun onDealloc(old: Owner) {
            zextMap.forEach { (k, v) ->
                k.dealloc(v)
            }
        }

        fun zextCompute(env: Env): Owner {
            val regSize = env.makeRegSize(size)
            val temp = env.forceAllocReg(Owner.Flags(Env.Use.SCALAR_AIRTHM, regSize, null, vxcc.Type.INT))
            reg.reducedAsReg(regSize).emitMov(env, temp.storage)
            temp.storage.emitStaticMask(env, temp.storage,  (2.0).pow(size).toLong() - 1)
            temp.canBeDepromoted = Owner.Flags(Env.Use.STORE, regSize, null, vxcc.Type.INT)
            return temp
        }

        override fun emitMov(env: Env, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitMov(env, dest)
        }

        override fun emitStaticMask(env: Env, dest: Storage, mask: Long) {
            // TODO: we can optimize this in some cases
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitStaticMask(env, dest, mask)
        }

        override fun reduced(env: Env, to: Int): Value =
            reducedStorage(env, to)

        override fun reducedStorage(env: Env, to: Int): Storage {
            if (to > size)
                throw Exception("reducedStorage() can not extend size!")

            if (to == size)
                return this

            return if (to in arrayOf(8, 16, 32, 64, 128, 256))
                reg.reducedStorage(env, to)
            else
                View(reg, to)
        }
    }

    /**
     * Returns an emittable register that maps to the lower x bits of the reg.
     * x can not be any value.
     */
    override fun reducedStorage(env: Env, to: Int): Storage =
        try {
            reducedAsReg(to)
        } catch (_: Exception) {
            View(this, to)
        }

    @Throws(Exception::class)
    fun reducedAsReg(to: Int): Reg =
        from(asIndex(), to)

    /**
     * Move into destination.
     * Truncate if destination smaller
     */
    override fun emitMov(env: Env, dest: Storage) =
        when (dest) {
            is Reg -> {
                if (dest.totalWidth > totalWidth)
                    throw Exception("Cannot move into destination with bigger size; use emitSignExtend() or emitZeroExtend() instead!")

                when (type) {
                    Type.GP,
                    Type.GP64EX -> when (dest.type) {
                        Type.GP,
                        Type.GP64EX -> {
                            val src = reducedAsReg(dest.totalWidth)
                            env.emit("mov ${dest.name}, ${src.name}")
                        }

                        else -> TODO("mov for src register type $type to dest ${dest.type}")
                    }

                    Type.MM -> when (dest.type) {
                        Type.MM -> {
                            env.emit("movq ${dest.name} ${name}")
                        }

                        else -> throw Exception("Cannot move MM vector into dest ${dest.type}; use emitVecExtract() or emitMov(env, dest.reduced(64))")
                    }

                    else -> TODO("mov for src register type $type to dest ${dest.type}")
                }
            }

            is View -> {
                if (dest.size > totalWidth)
                    throw Exception("Cannot move into destination with bigger size; use emitSignExtend() or emitZeroExtend() instead!")

                when (type) {
                    Type.MM -> {
                        if (dest.reg.type in arrayOf(Type.XMM, Type.XMM64)) {
                            val dst = dest.reg.reducedAsReg(128)
                            env.emit("movq2dq ${dst.name}, $name")
                        }
                        else {
                            TODO("mov from mmx to reg view $dest")
                        }
                    }

                    else -> TODO("mov from $this to reg view $dest")
                }
            }

            else -> TODO("mov from $this to $dest")
        }

    override fun emitStaticMask(env: Env, dest: Storage, mask: Long) {
        fun stupid() {
            val temp = env.forceAllocReg(Owner.Flags(Env.Use.SCALAR_AIRTHM, this.totalWidth, null, vxcc.Type.INT))
            emitMov(env, temp.storage)
            temp.storage.emitStaticMask(env, temp.storage, mask)
            temp.storage.emitMov(env, dest)
            env.dealloc(temp)
        }

        // TODO: there is `pand` (for mmx, sse* and avx*)

        if (this.type !in arrayOf(Type.GP, Type.GP64EX))
            throw Exception("Can only static mask register values which are stored in GP or GP64EX registers")

        when (dest) {
            is Reg -> {
                if (dest.type !in arrayOf(Type.GP, Type.GP64EX)) {
                    stupid()
                }
                else {
                    if (dest != this)
                        emitMov(env, dest)

                    env.emit("and ${dest.name}, $mask")
                }
            }

            // TODO Reg.View

            else -> {
                stupid()
            }
        }
    }

    override fun reduced(env: Env, to: Int): Value =
        reducedStorage(env, to)

    fun onDealloc(old: Owner) =
        views.forEach { it.onDealloc(old) }
}