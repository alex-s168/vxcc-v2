package vxcc.vxcc.x86

interface Storage: Value {
    /** try to convert to reg */
    fun asReg(): Reg =
        this as Reg

    /**
     * Returns a storage object that maps to the lower x bits of the storage.
     * x can not be any value.
     * returned value only exists as long as parent.
     */
    fun reducedStorage(env: X86Env, to: Int): Storage

    /**
     * Zeros out the storage.
     */
    fun emitZero(env: X86Env)

    companion object {
        fun none(): Storage =
            object : Storage {
                override fun emitMov(env: X86Env, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun reducedStorage(env: X86Env, to: Int): Storage =
                    throw Exception("Storage.none() used!!!!")

                override fun emitStaticMask(env: X86Env, dest: Storage, mask: Long) =
                    throw Exception("Storage.none() used!!!!")

                override fun reduced(env: X86Env, to: Int): Value =
                    throw Exception("Storage.none() used!!!!")

                override fun emitSignedMul(env: X86Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitStaticShiftLeft(env: X86Env, by: Long, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitShiftLeft(env: X86Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitMul(env: X86Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitAdd(env: X86Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitZero(env: X86Env) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitExclusiveOr(env: X86Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitSignedMax(env: X86Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitMask(env: X86Env, mask: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitStaticShiftRight(env: X86Env, by: Long, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitShiftRight(env: X86Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")
            }
    }
}