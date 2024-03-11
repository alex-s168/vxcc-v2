package vxcc

import kotlin.math.pow

// TODO: check destination size when operating
// TODO: the idiot designers of amd64 decided that changing e*x zeros out the top of r*x...

data class Reg(
    val name: String,
    val totalWidth: Int,
    val type: Type,
    val localId: Int,
): Storage {
    fun isGP() =
        this.type == Type.GP || this.type == Type.GP64EX

    fun asIndex(): Index =
        Index(type, localId)

    data class Index(
        val type: Type,
        val localId: Int,
    )

    // TODO: upper 8 bits of 16 bit gp registers

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
        fun fromName(name: String): Reg {
            when (name) {
                "ax" -> return Reg(name, 16, Type.GP, 0)
                "bx" -> return Reg(name, 16, Type.GP, 1)
                "cx" -> return Reg(name, 16, Type.GP, 2)
                "dx" -> return Reg(name, 16, Type.GP, 3)
                "si" -> return Reg(name, 16, Type.GP, 4)
                "di" -> return Reg(name, 16, Type.GP, 5)

                "al" -> return Reg(name, 8, Type.GP, 0)
                "bl" -> return Reg(name, 8, Type.GP, 1)
                "cl" -> return Reg(name, 8, Type.GP, 2)
                "dl" -> return Reg(name, 8, Type.GP, 3)
                "sil" -> return Reg(name, 8, Type.GP, 4)
                "dil" -> return Reg(name, 8, Type.GP, 5)
            }

            if (name.startsWith('e'))
                return Reg(name, 32, Type.GP, fromName(name.substring(1)).localId)

            if (name.startsWith('r')) {
                if (name.endsWith('b'))
                    return fromName(name.dropLast(1)).reducedAsReg(8)

                if (name.endsWith('w'))
                    return fromName(name.dropLast(1)).reducedAsReg(16)

                if (name.endsWith('d'))
                    return fromName(name.dropLast(1)).reducedAsReg(32)

                val substr = name.substring(1)
                return substr.toIntOrNull()?.let {
                    if (it !in 8..15)
                        throw Exception("Register $name does not exist!")
                    Reg(name, 64, Type.GP64EX, it - 8)
                } ?: Reg(name, 64, Type.GP, fromName(substr).localId)
            }

            if (name.startsWith("mm")) {
                val id = name.substring(2).toIntOrNull()
                    ?: throw Exception("Register $name does not exist!")
                if (id !in 0..7)
                    throw Exception("Register $name does not exist!")
                return Reg(name, 64, Type.MM, id)
            }

            val xmm = name.startsWith("xmm")
            val ymm = name.startsWith("ymm")
            val zmm = name.startsWith("zmm")
            if (xmm || ymm || zmm) {
                val id = name.substring(3).toIntOrNull()
                    ?: throw Exception("Register $name does not exist!")
                val size = if (xmm) 128 else if (ymm) 256 else 512
                return when (id) {
                    in 0..7 -> Reg(name, size, Type.XMM, id)
                    in 8..15 -> Reg(name, size, Type.XMM64, id)
                    in 16..23 -> {
                        if (!zmm) throw Exception("Register $name does not exist!")
                        Reg(name, 512, Type.ZMMEX, id)
                    }
                    else -> throw Exception("Register $name does not exist!")
                }
            }

            throw Exception("Register $name does not exist!")
        }

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
                64 -> Reg("r$id", 64, Type.GP64EX, localId)
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

        fun recompute() {
            zextMap.forEach { (k, v) ->
                k.dealloc(v)
            }
            zextMap.clear()
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

        override fun emitStaticShiftLeft(env: Env, by: Long, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitStaticShiftLeft(env, by, dest)
        }

        override fun emitShiftLeft(env: Env, other: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitShiftLeft(env, other, dest)
        }

        override fun emitShiftRight(env: Env, other: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitShiftRight(env, other, dest)
        }

        override fun emitStaticShiftRight(env: Env, by: Long, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitStaticShiftRight(env, by, dest)
        }

        override fun emitMul(env: Env, other: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitMul(env, other, dest)
        }

        override fun emitSignedMul(env: Env, other: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitSignedMul(env, other, dest)
        }

        override fun emitAdd(env: Env, other: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitAdd(env, other, dest)
        }

        override fun emitZero(env: Env) {
            reg.emitStaticMask(env, reg, (1L shl size).inv() and (1L shl reg.totalWidth))
            recompute()
        }

        override fun emitExclusiveOr(env: Env, other: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitExclusiveOr(env, other, dest)
        }

        override fun emitSignedMax(env: Env, other: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitSignedMax(env, other, dest)
        }

        override fun emitMask(env: Env, mask: Value, dest: Storage) {
            val zext = zextMap.computeIfAbsent(env, ::zextCompute)
            zext.storage.emitMask(env, mask, dest)
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
    override fun emitMov(env: Env, dest: Storage) {
        if (dest == this)
            return

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

                // TODO: call recompute() on view afterwards
                when (type) {
                    Type.MM -> {
                        if (dest.reg.type in arrayOf(Type.XMM, Type.XMM64)) {
                            val dst = dest.reg.reducedAsReg(128)
                            env.emit("movq2dq ${dst.name}, $name")
                        } else {
                            TODO("mov from mmx to reg view $dest")
                        }
                    }

                    else -> TODO("mov from $this to reg view $dest")
                }
            }

            else -> TODO("mov from $this to $dest")
        }
    }

    override fun emitStaticMask(env: Env, dest: Storage, mask: Long) {
        if (!this.isGP())
            throw Exception("Can only static mask register values which are stored in GP or GP64EX registers")

        dest.useInGPRegWriteBack(env, copyInBegin = true) { destReg ->
            env.emit("and ${destReg.name}, $mask")
        }
    }

    override fun reduced(env: Env, to: Int): Value =
        reducedStorage(env, to)

    fun onDealloc(old: Owner) =
        views.forEach { it.recompute() }

    private fun binaryOp0(env: Env, other: Value, dest: Storage, op: String) {
        if (!this.isGP())
            throw Exception("Can only perform scalar scalar binary op with GP reg!")

        if (dest == this) {
            when (other) {
                is Immediate -> env.emit("$op $name, ${other.value}")
                else -> other.useInGPReg(env) { reg ->
                    env.emit("$op $name, ${reg.name}")
                }
            }
        }
        else {
            dest.useInGPRegWriteBack(env, copyInBegin = false) { reg ->
                if (!reg.isGP())
                    throw Exception("Can only perform scalar binary op into GP reg!")
                emitMov(env, reg)
                reg.binaryOp0(env, other, reg, op)
            }
        }
    }

    override fun emitAdd(env: Env, other: Value, dest: Storage) =
        binaryOp0(env, other, dest, "add")

    override fun emitStaticShiftLeft(env: Env, by: Long, dest: Storage) =
        emitShiftLeft(env, env.immediate(by, totalWidth), dest)

    override fun emitStaticShiftRight(env: Env, by: Long, dest: Storage) =
        emitShiftRight(env, env.immediate(by, totalWidth), dest)

    override fun emitMul(env: Env, other: Value, dest: Storage) =
        if (this.isGP() && this.localId == 0 && other is Reg) // al, ax, eax, rax
            env.emit("mul ${other.name}")
        else
            emitSignedMul(env, other, dest)

    override fun emitSignedMul(env: Env, other: Value, dest: Storage) {
        if (!this.isGP())
            throw Exception("Can only perform scalar scalar binary op with GP reg!")

        when (other) {
            is Immediate -> dest.useInGPRegWriteBack(env, copyInBegin = false) { destReg ->
                env.emit("imul ${destReg.name}, ${this.name}, ${other.value}")
            }
            else -> other.useInGPReg(env) { reg ->
                if (this == dest) {
                    env.emit("imul ${this.name}, ${reg.name}")
                } else {
                    dest.useInGPRegWriteBack(env, copyInBegin = false) { destReg ->
                        emitMov(env, destReg)
                        env.emit("imul ${destReg.name}, ${reg.name}")
                    }
                }
            }
        }
    }

    override fun emitShiftLeft(env: Env, other: Value, dest: Storage) =
        binaryOp0(env, other, dest, "shl")

    override fun emitShiftRight(env: Env, other: Value, dest: Storage) =
        binaryOp0(env, other, dest, "shr")

    override fun emitZero(env: Env) =
        when (type) {
            Type.GP,
            Type.GP64EX ->
                if (totalWidth == 64)
                    reducedAsReg(32).let { env.emit("xor ${it.name}, ${it.name}") }
                else
                    env.emit("xor $name, $name")

            Type.MM ->
                env.emit("pxor $name, $name")

            Type.XMM,
            Type.XMM64,
            Type.ZMMEX ->
                if (env.optMode == Env.OptMode.SIZE)
                    env.emit("xorps $name, $name")
                else
                    env.emit("pxor $name, $name")
        }

    private fun emitCwdCdqCqo(env: Env) {
        assert(this.type == Type.GP && this.localId == 0)
        when (this.totalWidth) {
            8 -> throw Exception("cwd/cdq/cqo not available for 8 bit regs")
            16 -> env.emit("cwd")
            32 -> env.emit("cdq")
            64 -> env.emit("cqo")
            else -> throw Exception("wtf")
        }
    }

    override fun emitSignedMax(env: Env, other: Value, dest: Storage) {
        when (other) {
            is Immediate -> {
                if (other.value == 0L && dest is Reg && dest.type == Type.GP && dest.localId == 0) {
                    val edx = env.allocReg(
                        env.getRegByIndex(Index(Type.GP, 2)),
                        Owner.Flags(Env.Use.SCALAR_AIRTHM, this.totalWidth, null, vxcc.Type.UINT)
                    )
                    if (edx == null) {
                        useInGPReg(env) {
                            this.emitSignedMax(env, it, dest)
                        }
                    } else {
                        if (this != dest)
                            emitMov(env, dest)
                        // cdq
                        // and edx, eax
                        // xor eax, edx
                        emitCwdCdqCqo(env)
                        edx.storage.emitMask(env, this, edx.storage)
                        this.emitExclusiveOr(env, edx.storage, this)
                        env.dealloc(edx)
                    }
                } else {
                    useInGPReg(env) {
                        this.emitSignedMax(env, it, dest)
                    }
                }
            }

            else -> {
                if (!this.isGP())
                    throw Exception("Can only perform scalar signed max on GP regs!")

                dest.useInGPRegWriteBack(env) { dreg ->
                    other.useInGPReg(env) { reg ->
                        if (reg != dreg)
                            env.emit("mov ${dreg.name}, ${reg.name}")
                        env.emit("cmp $name, ${reg.name}")
                        env.emit("cmovl $name, ${dreg.name}")
                    }
                }
            }
        }
    }

    override fun emitExclusiveOr(env: Env, other: Value, dest: Storage) =
        binaryOp0(env, other, dest, "xor")

    override fun emitMask(env: Env, mask: Value, dest: Storage) =
        binaryOp0(env, mask, dest, "and")
}