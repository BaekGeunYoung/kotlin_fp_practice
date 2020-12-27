package testing

import RNG
import SimpleRNG

typealias TestCases = Int
typealias MaxSize = Int

class Prop(
    val run: (MaxSize, TestCases, RNG) -> Result
) {
    fun and(p: Prop): Prop =
        Prop { maxSize, testCases, rng ->
            if (this@Prop.run(maxSize, testCases, rng) is Passed && p.run(maxSize, testCases, rng) is Passed) Passed()
            else if (this@Prop.run(maxSize, testCases, rng) is Falsified) this@Prop.run(maxSize, testCases, rng)
            else p.run(maxSize, testCases, rng)
        }
}

fun run(p: Prop, maxSize: Int = 100, testCases: Int = 100, rng: RNG = SimpleRNG(System.currentTimeMillis())) =
    p.run(maxSize, testCases, rng).let { result ->
        when(result) {
            is Falsified -> println("! Falsified after ${result.successes} passed tests:\n ${result.failure}")
            is Passed -> println("+ OK, passed $testCases tests")
        }
    }
