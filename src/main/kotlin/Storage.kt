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
            }
    }
}