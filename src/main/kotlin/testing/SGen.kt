package testing

import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.math.min
import kotlin.streams.toList

class SGen<T>(
    val forSize: (Int) -> Gen<T>
) {
    fun <U> map(f: (T) -> U): SGen<U> = SGen { forSize(it).map(f) }
    fun <U> flatMap(f: (T) -> SGen<U>): SGen<U> = SGen { size ->
        forSize(size).flatMap {
            f(it).forSize(size)
        }
    }

    companion object {
        fun <A> listOf(g: Gen<A>): SGen<List<A>> =
            SGen {
                g.listOfN(it)
            }
    }
}

fun <A> forAll(g: SGen<A>, f: (A) -> Boolean): Prop = forAll(g.forSize, f)

fun <A> forAll(g: (Int) -> Gen<A>, f: (A) -> Boolean): Prop = Prop { maxSize, testCases, rng ->
    val casesPerSize = (testCases + maxSize - 1) / maxSize
    val props: Stream<Prop> = IntStream.range(0, min(testCases, maxSize)).boxed().map { forAll(g(it), f) }

    val prop: Prop =
        props.map { p ->
            Prop { max, n, rng2 ->
                p.run(max, casesPerSize, rng)
            }
        }.toList().reduce { a, b -> a.and(b) }

    prop.run(maxSize, testCases, rng)
}