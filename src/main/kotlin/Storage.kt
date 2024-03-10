package vxcc

interface Storage: Value {
    /** try to convert to reg */
    fun asReg(): Reg =
        this as Reg

    /**
     * Returns a storage object that maps to the lower x bits of the storage.
     * x can not be any value.
     * returned value only exists as long as parent.
     */
    fun reducedStorage(env: Env, to: Int): Storage

    /**
     * Zeros out the storage.
     */
    fun emitZero(env: Env)

    companion object {
        fun none(): Storage =
            object : Storage {
                override fun emitMov(env: Env, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun reducedStorage(env: Env, to: Int): Storage =
                    throw Exception("Storage.none() used!!!!")

                override fun emitStaticMask(env: Env, dest: Storage, mask: Long) =
                    throw Exception("Storage.none() used!!!!")

                override fun reduced(env: Env, to: Int): Value =
                    throw Exception("Storage.none() used!!!!")

                override fun emitSignedMul(env: Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitStaticShiftLeft(env: Env, by: Long, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitShiftLeft(env: Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitMul(env: Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitAdd(env: Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitZero(env: Env) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitExclusiveOr(env: Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitSignedMax(env: Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitMask(env: Env, mask: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitStaticShiftRight(env: Env, by: Long, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")

                override fun emitShiftRight(env: Env, other: Value, dest: Storage) =
                    throw Exception("Storage.none() used!!!!")
            }
    }
}