package monoid

import paralellism.Par
import paralellism.map2
import paralellism.unit
import testing.Gen
import testing.Prop
import testing.forAll
import java.util.Optional

interface Monoid <A> {
    val zero: A
    fun op(a1: A, a2: A): A
}

val intAddition: Monoid<Int> = object: Monoid<Int> {
    override val zero: Int = 0
    override fun op(a1: Int, a2: Int): Int = a1 + a2
}

val intMultiplication: Monoid<Int> = object : Monoid<Int> {
    override val zero: Int = 1
    override fun op(a1: Int, a2: Int): Int = a1 * a2
}

val booleanOr = object : Monoid<Boolean> {
    override val zero: Boolean = false
    override fun op(a1: Boolean, a2: Boolean): Boolean = a1 || a2
}

val booleanAnd = object : Monoid<Boolean> {
    override val zero: Boolean = true
    override fun op(a1: Boolean, a2: Boolean): Boolean = a1 && a2
}

fun <A> optionMonoid() = object : Monoid<Optional<A>> {
    override val zero: Optional<A> = Optional.empty()
    override fun op(a1: Optional<A>, a2: Optional<A>): Optional<A> = a1.or { a2 }
}

fun <A> endoMonoid() = object : Monoid<(A) -> A> {
    override val zero: (A) -> A = { it }
    override fun op(a1: (A) -> A, a2: (A) -> A): (A) -> A = {
        a2(a1(it))
    }
}

fun <A> monoidLaws(m: Monoid<A>, gen: Gen<Triple<A, A, A>>): Prop =
    forAll(gen) { (a, b, c) ->
        m.op(m.op(a, b), c) == m.op(a, m.op(b, c))
    }.and(
        forAll(gen) { (a, _, _) ->
            m.op(a, m.zero) == a && m.op(m.zero, a) == a
        }
    )

fun main() {
    val stringConcatMonoid: Monoid<String> = object : Monoid<String> {
        override val zero: String = ""

        override fun op(a1: String, a2: String): String = a1 + a2

    }

    val result = listOf("abc", "def", "ghi").foldRight(stringConcatMonoid.zero) { x, y ->
        stringConcatMonoid.op(x, y)
    }

    val f = { it: Int -> it.toString() + it.toString() + it.toString()}

    val foldMapTest = foldMap(listOf(1, 2, 3), stringConcatMonoid) {
        f(it)
    }

    val equivalent = listOf(1, 2, 3).foldRight("") { x, y ->
        f(x) + y
    }

    println(foldMapTest)
    println(equivalent)
}

fun <A, B> foldMap(aSeq: List<A>, m: Monoid<B>, f: (A) -> B): B =
    aSeq.map(f).foldRight(m.zero) { x, y ->
        m.op(x, y)
    }

fun <A, B> foldMapV(aSeq: List<A>, m: Monoid<B>, f: (A) -> B): B {
    val left = foldMapV(aSeq.subList(0, aSeq.size / 2), m, f)
    val right = foldMapV(aSeq.subList(aSeq.size / 2, aSeq.size), m, f)

    return m.op(left, right)
}

fun <A, B> foldRight(aSeq: List<A>, z: B, f: (A, B) -> B): B =
    foldMap(aSeq, endoMonoid()) {
        f.curried()(it)
    }(z)

fun <A, B> ((A, B) -> B).curried(): (A) -> (B) -> B = { a ->
    { b ->
        this(a, b)
    }
}

fun <A> par(m: Monoid<A>): Monoid<Par<A>> = object : Monoid<Par<A>> {
    override val zero = unit(m.zero)
    override fun op(a1: Par<A>, a2: Par<A>): Par<A> {
        return map2(a1, a2) { a, b ->
            m.op(a, b)
        }
    }
}

fun <A, B> parFoldMap(aSeq: List<A>, m: Monoid<B>, f: (A) -> B): Par<B> {
    if (aSeq.size == 1) return unit(f(aSeq[0]))

    val parMonoid = par(m)

    val left = parFoldMap(aSeq.subList(0, aSeq.size / 2), m, f)
    val right = parFoldMap(aSeq.subList(aSeq.size / 2, aSeq.size), m, f)

    return parMonoid.op(left, right)
}