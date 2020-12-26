package testing

import RNG
import Rand
import SimpleRNG
import flatMap
import gorgeousRandomDouble
import map
import nonNegativeInt


class Gen<T>(
    val rand: Rand<T>
) {
    fun <U> flatMap(f: (T) -> Gen<U>): Gen<U> {
        val newRand = this.rand.flatMap {
            f(it).rand
        }
        return Gen(newRand)
    }

    fun listOfN(size: Gen<Int>): Gen<List<T>> {
        return size.flatMap { listOfN(it, this) }
    }

    companion object {
        fun choose(start: Int, stopExclusive: Int): Gen<Int> {
            val rand = map(nonNegativeInt) {
                start + it % (stopExclusive - start)
            }
            return Gen(rand)
        }

        fun boolean(): Gen<Boolean> {
            val rand: Rand<Boolean> = {
                (it.next().first % 2 == 0) to it.next().second
            }
            return Gen(rand)
        }

        fun double(): Gen<Double> {
            return Gen(gorgeousRandomDouble)
        }

        fun <T> listOfN(n: Int, gen: Gen<T>): Gen<List<T>> {
            val rand: Rand<List<T>> = { rng ->
                var localRng = gen.rand(rng).second

                (0 until n).map {
                    val result = gen.rand(localRng).first
                    localRng = gen.rand(localRng).second
                    result
                } to rng.next().second
            }
            return Gen(rand)
        }

        fun <T> unit(t: () -> T): Gen<T> {
            val rand: Rand<T> = { rng ->
                t() to rng
            }
            return Gen(rand)
        }

        fun <A> union(g1: Gen<A>, g2: Gen<A>): Gen<A> {
            return boolean().flatMap { if (it) g1 else g2 }
        }

        fun <A> weighted(g1: Pair<Gen<A>, Double>, g2: Pair<Gen<A>, Double>): Gen<A> {
            return double().flatMap { if (it < g1.second / (g1.second + g2.second)) g1.first else g2.first }
        }
    }
}

fun main() {
    var rng: RNG = SimpleRNG(System.currentTimeMillis())
    val randomChoose = Gen.choose(0, 100)
    val randomBoolean = Gen.boolean()
    val randomList = Gen.listOfN(10, randomChoose)

//    repeat(100) {
//        println(randomChoose.rand(rng).first)
//        rng = randomChoose.rand(rng).second
//    }
//
//    repeat(100) {
//        println(randomBoolean.rand(rng).first)
//        rng = randomChoose.rand(rng).second
//    }
//
//    repeat(100) {
//        println(randomList.rand(rng).first)
//        rng = randomChoose.rand(rng).second
//    }
//
    val zeroToTen = Gen.choose(0, 1)
    val tenToTwenty = Gen.choose(1, 2)
    val zeroToTwenty = Gen.weighted(zeroToTen to 0.7, tenToTwenty to 0.3)

    repeat(100) {
        println(zeroToTwenty.rand(rng).first)
        rng = zeroToTwenty.rand(rng).second
    }
}