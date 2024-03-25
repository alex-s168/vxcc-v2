package vxcc.cg

interface AbstractScalarValue<E: CGEnv<E>>: Value<E> {
    override fun emitShuffle(env: E, selection: IntArray, dest: Storage<E>) =
        throw Exception("Can not perform vector operation on scalar value!")
}