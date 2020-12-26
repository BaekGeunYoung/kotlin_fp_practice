import kotlin.math.abs

interface RNG {
    fun next(): Pair<Int, RNG>
}


class SimpleRNG(
    private val seed: Long
): RNG {
    override fun next(): Pair<Int, RNG> {
        val newSeed = (seed * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
        val nextRNG = SimpleRNG(newSeed)
        val n = (newSeed shr 16).toInt()
        return Pair(n, nextRNG)
    }
}

fun main() {
    var rng: RNG = SimpleRNG(System.currentTimeMillis())

    repeat(100) {
        println(gorgeousRandomDouble(rng).first)
        rng = gorgeousRandomDouble(rng).second
    }
}

val nonNegativeInt: Rand<Int> = { rng ->
    val (number, nextRng) = rng.next()
    Pair(abs(number), nextRng)
}

typealias State<S, A> = (S) -> Pair<A, S>

typealias Rand<A> = State<RNG, A>

fun <A, B> map(s: Rand<A>, f: (A) -> B): Rand<B> {
    return { rng ->
        val (a, rng2) = s(rng)
        Pair(f(a), rng2)
    }
}

fun <A, B> Rand<A>.flatMap(f: (A) -> Rand<B>): Rand<B> {
    return { rng ->
        val (a, nextRng) = this(rng)
        f(a)(nextRng)
    }
}

val stringGenerator: Rand<String> = {
    val next = it.next()
    next.first.toString() to next.second
}

val stringListGenerator = {
    map(stringGenerator) {
        it.toCharArray()
    }
}

val nonNegativeEven: Rand<Int> = map(nonNegativeInt) { it - it % 2 }

fun <A> rngTester(rand: Rand<A>) {
    val simpleRng = SimpleRNG(42)
    val (int2, rng2) = rand(simpleRng)
    val (int3, rng3) = rand(rng2)
    val (int4, rng4) = rand(rng3)
    val (int5, rng5) = rand(rng4)

    println(int2)
    println(int3)
    println(int4)
    println(int5)
    println("---------------")
}

val randomDouble = { rng: RNG ->
    val (value, rng2) = rng.next()
    Pair(abs(value.toDouble()) / Int.MAX_VALUE, rng2)
}

val gorgeousRandomDouble: Rand<Double> = map(nonNegativeInt) { it.toDouble() / Int.MAX_VALUE }

fun <A, B, C> map2(ra: Rand<A>, rb: Rand<B>, f: (A, B) -> C): Rand<C> =
    { rng ->
        val (value1, rng1) = ra(rng)
        val (value2, rng2) = rb(rng1)
        Pair(f(value1, value2), rng2)
    }

fun <A, B> both(ra: Rand<A>, rb: Rand<B>): Rand<Pair<A,B>> = map2(ra, rb) { a, b -> Pair(a, b) }
//
//fun <A> sequence(rs: List<Rand<A>>): Rand<List<A>> {
//    if (rs.isEmpty()) return {
//        Pair(listOf(), it)
//    }
//    else return {
//        rs.foldRight(listOf<A>()) { t, acc ->
//            paralellism.map2(t, acc) {}
//        }
//        val a: List<A> = rs.map { rand -> rand(it).first }
//        val b: List<RNG> = rs.map { rand -> rand(it).second }
//        Pair(a, b)
//    }
//}

//fun ints(count: Int): Rand<List<Int>> = sequence(List.fill(count)(int))

fun <A, B, C> curry(f: (A, B) -> C): (A) -> ((B) -> C) =
        { a ->
            { b -> f(a,b) }
        }

fun curryTest() {
    val f: (Int, Int) -> String = { a, b -> a.toString().plus(b.toString()) }
    val a = 123
    val b = 456

    // naive use of f
    println(f(a, b))

    // use of curried f
    println(curry(f)(a)(b))

    // advantage of currying function example
    val fixedPrefixFunction = curry(f)(123)

    println(fixedPrefixFunction(456))
}

fun <A, B, C> uncurry(f: (A) -> ((B) -> C)): (A, B) -> C =
        { a, b ->
            f(a)(b)
        }

fun <A, B, C> compose(f: (B) -> C, g: (A) -> B): (A) -> C = { f(g(it)) }

fun sum(ints: List<Int>): Int {
    return if (ints.isEmpty()) 0
    else ints[0] + sum(ints.subList(1, ints.size))
}

fun product(ds: List<Double>): Double {
    return if (ds.isEmpty()) 1.0
    else ds[0] * product(ds.subList(1, ds.size))
}

fun <A, B> foldRight(aSeq: List<A>, z: B, f: (A, B) -> B): B {
    return if (aSeq.isEmpty()) z
    else f(aSeq[0], foldRight(aSeq.subList(1, aSeq.size), z, f))
}

fun sum2(ints: List<Int>): Int = foldRight(ints, 0, {a, b -> a + b})

fun product2(ds: List<Double>): Double = foldRight(ds, 1.0, {a, b -> a * b})