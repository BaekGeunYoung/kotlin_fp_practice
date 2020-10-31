sealed class FList<out A> {
    companion object {

    }
}

object Nil : FList<Nothing>()

data class Cons<out A>(val head: A, val tail: FList<A>) : FList<A>() {
    override fun toString(): String {
        return "[" + foldRight(this, "", {a, b -> "$a, $b"}) + "]"
    }
}

fun <A, B> foldRight(aSeq: FList<A>, z: B, f: (A, B) -> B): B =
        when (aSeq) {
            is Nil -> z
            is Cons -> f(aSeq.head, foldRight(aSeq.tail, z, f))
        }

fun sum3(ints: FList<Int>): Int = foldRight(ints, 0, {a, b -> a + b})

fun <A> length(aSeq: FList<A>): Int = foldRight(aSeq, 0, {_, b -> b + 1})


fun <A, B> foldLeft(aSeq: FList<A>, z: B, f: (B, A) -> B): B =
        when (aSeq) {
            is Nil -> z
            is Cons -> f(foldLeft(aSeq.tail, z, f), aSeq.head)
        }

fun sum4(ints: FList<Int>): Int = foldLeft(ints, 0, {b, a -> b + a})

fun product4(ds: FList<Double>): Double = foldLeft(ds, 1.0, {b, a -> b * a})

fun <A> append(aSeq: FList<A>, bSeq: FList<A>): FList<A> {
    return foldRight(aSeq, bSeq, { a, b -> Cons(a, b)})
}

fun <A> flatten(ass: FList<FList<A>>): FList<A> =
        foldRight(ass, Nil) { aSeq: FList<A>, b: FList<A> ->
            append(aSeq, b)
        }

fun plusOne(ints: FList<Int>): FList<Int> = foldRight(ints, Nil) { a: Int, b: FList<Int> -> Cons(a + 1, b) }

fun <A, B> map(aSeq: FList<A>, f: (A) -> B): FList<B> = foldRight(aSeq, Nil) { a: A, b: FList<B> -> Cons(f(a), b) }

fun <A> filter(aSeq: FList<A>, f: (A) -> Boolean): FList<A> = foldRight(aSeq, Nil) { a: A, b: FList<A> -> if (f(a)) Cons(a, b) else b }

fun <A, B> flatMap(aSeq: FList<A>, f: (A) -> FList<B>): FList<B> = flatten(map(aSeq, f))

fun <A> index(aSeq: FList<A>): FList<Pair<A, Int>> = foldRight(aSeq, Nil) { a: A, b: FList<Pair<A, Int>> -> Cons(Pair(a, length(b) + 1), b)}

fun zipInt(ints1: FList<Int>, ints2: FList<Int>) = map(index(ints1)) { int1 ->
    val corresponding = when (val int2Item = filter(index(ints2)) { int2 -> int1.second == int2.second }) {
        is Nil -> 0
        is Cons -> int2Item.head.first
    }

    int1.first + corresponding
}

fun zipInt2(ints1: FList<Int>, ints2: FList<Int>): FList<Int> {
    return when (ints1) {
        is Cons -> when (ints2) {
            is Cons -> Cons(ints1.head + ints2.head, zipInt2(ints1.tail, ints2.tail))
            is Nil -> Nil
        }
        is Nil -> Nil
    }
}

fun main() {
    val ints1 = Cons(1, Cons(2, Cons(3, Cons(4, Nil))))
    val ints2 = Cons(1, Cons(2, Cons(3, Cons(4, Nil))))
    println(ints1)
    println(ints2)
    println(append(ints1, ints2))

    val intss: FList<FList<Int>> = Cons(ints1, Cons(ints2, Nil))

    println(intss)
    println(flatten(intss))

    println(plusOne(ints1))

    println(map(ints1) { it * 10 })

    println(map(ints1) { Cons(it + 1, Cons(it + 2, Cons(it + 3, Nil))) })

    val flatMapInts = flatMap(ints1) { Cons(it + 1, Cons(it + 2, Cons(it + 3, Nil))) }
    println(flatMapInts)

    println(filter(flatMapInts) { it % 2 == 0 })

    println(index(flatMapInts))

    println(zipInt(ints1, ints2))
    println(zipInt2(ints1, ints2))
}
