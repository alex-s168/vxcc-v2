package vxcc

private fun Env.emitSize(stride: Long) =
    when (stride) {
        1L -> emit("byte")
        2L -> emit("word")
        4L -> emit("dword")
        8L -> emit("qword")
        16L -> emit("oword")
        else -> throw Exception("Invalid stride (impl error)")
    }

data class ArrIndex(
    val arr: Value,
    val index: Value,
    val stride: Long, // when stride is 1, 2, 4 or 8 -> lea [arr + index * stride], otherwise fma or shifting or mult
) {
    companion object {
        // both regs need to be GP or GP64EX !!!!!!!
        private fun emitAddrSelectorDirect(env: Env, arr: Reg, index: Reg, stride: Long) {
            env.emit("[${arr.name} + ${index.name} * $stride]")
        }
    }

    internal fun emitOffset(env: Env, dest: Storage) {
        TODO()
    }

    internal fun emitIndex(env: Env, dest: Storage) {
        TODO()
    }
}

data class Increment(
    val value: Value,
    val by: Long,
)

data class Decrement(
    val value: Value,
    val by: Long,
)

data class Multiply(
    val value: Value,
    val by: Long,
)