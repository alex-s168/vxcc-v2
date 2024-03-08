package vxcc

data class Obj<T>(val v: T)

fun <I, O> Obj<I>?.mapNotNull(transform: (I) -> O): Obj<O>? =
    this?.v?.let { Obj(transform(it)) }

fun <I, O> Obj<I>.map(transform: (I) -> O): Obj<O> =
    Obj(transform(v))

data class MutObj<T>(var v: T)

typealias Provider<T> = () -> T

class Either<A, B> private constructor(
    private val a: Obj<A>?,
    private val b: Obj<B>?
) {
    fun getAOrNull(): A? =
        a?.v

    fun getA(): A =
        (a ?: throw Exception("Value of Either is not of type A!")).v

    fun getAOr(prov: Provider<A>): A =
        getAOrNull() ?: prov()

    fun getBOrNull(): B? =
        b?.v

    fun getB(): B =
        (b ?: throw Exception("Value of Either is not of type B!")).v

    fun getBOr(prov: Provider<B>): B =
        getBOrNull() ?: prov()

    val isA: Boolean =
        a != null

    val isB: Boolean =
        b != null

    fun <R> then(af: (A) -> R, bf: (B) -> R): R =
        if (isA) af(a!!.v) else bf(b!!.v)

    fun <RA> mapA(transform: (A) -> RA): Either<RA, B> =
        Either(a.mapNotNull(transform), b)

    fun <RB> mapB(transform: (B) -> RB): Either<A, RB> =
        Either(a, b.mapNotNull(transform))

    override fun toString(): String =
        if (isA) "Either<A>($a)"
        else "Either<B>($b)"

    companion object {
        fun <A, B> ofA(a: A): Either<A, B> =
            Either(Obj(a), null)

        fun <A, B> ofB(b: B): Either<A, B> =
            Either(null, Obj(b))
    }
}

fun <A, B, R> Either<A, B>.commonize(): R where A: R, B: R =
    getAOrNull() ?: getB()