import kotlin.math.abs

interface RNG {
    fun nextInt(): Pair<Int, RNG>
}

class SimpleRNG(
    private val seed: Long
): RNG {
    override fun nextInt(): Pair<Int, RNG> {
        val newSeed = (seed * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
        val nextRNG = SimpleRNG(newSeed)
        val n = (newSeed shr 16).toInt()
        return Pair(n, nextRNG)
    }
}

fun main() {
    rngTester(nonNegativeInt)
    rngTester(nonNegativeEven)
    rngTester(randomDouble)
    rngTester(gorgeousRandomDouble)
}

val nonNegativeInt: Rand<Int> = { rng ->
    val (number, nextRng) = rng.nextInt()
    Pair(abs(number), nextRng)
}

typealias Rand<A> = (RNG) -> Pair<A, RNG>

fun <A, B> map(s: Rand<A>, f: (A) -> B): Rand<B> {
    return { rng ->
        val (a, rng2) = s(rng)
        Pair(f(a), rng2)
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
    val (value, rng2) = rng.nextInt()
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
//            map2(t, acc) {}
//        }
//        val a: List<A> = rs.map { rand -> rand(it).first }
//        val b: List<RNG> = rs.map { rand -> rand(it).second }
//        Pair(a, b)
//    }
//}

//fun ints(count: Int): Rand<List<Int>> = sequence(List.fill(count)(int))