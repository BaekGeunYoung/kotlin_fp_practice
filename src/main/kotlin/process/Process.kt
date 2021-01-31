package process

import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList

sealed class Process<I, O> {
    abstract fun apply(s: Stream<I>): Stream<O>

    fun repeat(): Process<I, O> {
        fun go(p: Process<I, O>): Process<I, O> = when (p) {
            is Halt -> go(this)
            is Await -> Await {
                if (it.isPresent) {
                    go(p.recv(it))
                }
                else {
                    p.recv(Optional.empty())
                }
            }
            is Emit -> Emit(p.head, go(p.tail))
        }

        return go(this)
    }

    fun <O2> pipe(p2: Process<O, O2>): Process<I, O2> =
        when (p2) {
            is Halt -> Halt()
            is Emit -> Emit(p2.head, this.pipe(p2.tail))
            is Await -> when (this) {
                is Halt -> this.pipe(p2.recv(Optional.empty()))
                is Emit -> this.tail.pipe(p2.recv(Optional.of(this.head)))
                is Await -> Await { this.recv(it).pipe(p2) }
            }
        }

    fun <O2> map(f: (O) -> O2): Process<I, O2> = this.pipe(lift(f))

    fun append(p: () -> Process<I, O>): Process<I, O> = when (this) {
        is Halt -> p()
        is Emit -> Emit(this.head, this.tail.append(p))
        is Await -> Await { this.recv(it).append(p) }
    }

    fun <O2> flatMap(f: (O) -> Process<I, O2>): Process<I, O2> = when (this) {
        is Halt -> Halt()
        is Emit -> f(this.head).append { this.tail.flatMap(f) }
        is Await -> Await { this.recv(it).flatMap(f) }
    }

    fun zipWithIndex(): Process<I, Pair<O, Int>> =
        zip(this, countUsingLoop())
}

class Emit<I, O>(
    val head: O,
    val tail: Process<I, O> = Halt()
) : Process<I, O>() {
    override fun apply(s: Stream<I>): Stream<O> = Stream.concat(listOf(head).stream(), tail.apply(s))
}

class Await<I, O>(
    val recv: (Optional<I>) -> Process<I, O>
) : Process<I, O>() {
    override fun apply(s: Stream<I>): Stream<O> =
        s.toList().let { sList ->
            sList.stream().findFirst().let { head ->
                if (head.isPresent) {
                    recv(head).apply(sList.stream().skip(1))
                }
                else {
                    recv(Optional.empty()).apply(sList.stream())
                }
            }
        }
}

class Halt<I, O> : Process<I, O>() {
    override fun apply(s: Stream<I>): Stream<O> = Stream.empty()
}

fun <I, O> liftOne(f: (I) -> O): Process<I, O> =
    Await {
        if (it.isPresent) {
            Emit(f(it.get()))
        }
        else {
            Halt()
        }
    }

fun <I, O> lift(f: (I) -> O): Process<I, O> = liftOne(f).repeat()

fun <I> filter(p: (I) -> Boolean): Process<I, I> {
    val await: Process<I, I> = Await {
        if (it.isPresent) {
            if (p(it.get())) Emit(it.get())
            else Halt()
        }
        else Halt()
    }

    return await.repeat()
}

fun sum(): Process<Double, Double> {
    fun go(acc: Double): Process<Double, Double> = Await {
        if (it.isPresent) Emit(it.get() + acc, go(it.get() + acc))
        else Halt()
    }

    return go(0.0)
}

fun <I> take(n: Int): Process<I, I> =
    if (n == 0)
        Halt()
    else {
        Await {
            if (it.isPresent) Emit(it.get(), take(n - 1))
            else Halt()
        }
    }

fun <I> drop(n: Int): Process<I, I> =
    if (n > 0) Await {
        if (it.isPresent) drop(n - 1)
        else Halt()
    }

    else {
        Await {
            if (it.isPresent) Emit(it.get(), drop(n - 1))
            else Halt()
        }
    }


fun <I> takeWhile(f: (I) -> Boolean): Process<I, I> =
    Await {
        if (it.isPresent) {
            if (f(it.get())) Emit(it.get(), takeWhile(f))
            else Halt()
        }
        else Halt()
    }

fun <I> id(): Process<I, I> = Await {
    if (it.isPresent) {
        Emit(it.get(), id())
    }
    else Halt()
}


fun <I> dropWhile(f: (I) -> Boolean): Process<I, I> =
    Await {
        if (it.isPresent) {
            if (f(it.get())) dropWhile(f)
            else Emit(it.get(), id())
        }
        else Halt()
    }

fun <I> count(): Process<I, Int> {
    fun go(curr: Int): Process<I, Int> = Await {
        if (it.isPresent) Emit(curr, go(curr + 1))
        else Halt()
    }

    return go(1)
}

fun mean(): Process<Double, Double> {
    fun go(idx: Int, acc: Double): Process<Double, Double> = Await {
        if (it.isPresent) Emit((it.get() + acc) / idx, go(idx + 1, it.get() + acc))
        else Halt()
    }

    return go(1, 0.0)
}

fun <S, I, O> loop(z: S, f: (I, S) -> Pair<O, S>): Process<I, O> =
    Await {
        if (it.isPresent) Emit(f(it.get(), z).first, loop(f(it.get(), z).second, f))
        else Halt()
    }

fun sumUsingLoop(): Process<Double, Double> = loop(0.0) { i, s ->
    i + s to i + s
}

fun <I> countUsingLoop(): Process<I, Int> = loop(1) { _, s ->
    s to s + 1
}

fun meanUsingLoop(): Process<Double, Double> = loop(1 to 0.0) { i, s ->
    (s.second + i) / s.first to (s.first + 1 to s.second + i)
}

fun <A, B, C> zip(p1: Process<A,B>, p2: Process<A,C>): Process<A, Pair<B,C>> =
    if (p1 is Halt || p2 is Halt) Halt()
    else if (p1 is Emit && p2 is Emit) Emit((p1.head to p2.head), zip(p1.tail, p2.tail))
    else if (p1 is Await) Await<A, Pair<B, C>> {
        zip(p1.recv(it), feed(it, p2))
    }
    else if (p2 is Await) Await<A, Pair<B, C>> {
        zip(feed(it, p1), p2.recv(it))
    }
    else error("unreachable code block")

fun <A, B> feed(oa: Optional<A>, p: Process<A, B>): Process<A, B> =
    when (p) {
        is Halt -> p
        is Emit -> Emit(p.head, feed(oa, p.tail))
        is Await -> p.recv(oa)
    }

fun <I> exists(f: (I) -> Boolean): Process<I, Boolean> =
    Await {
        if (it.isPresent) {
            if (f(it.get())) Emit(true, exists(f))
            else Emit(false, exists(f))
        }
        else Halt()
    }

fun main() {
    val even = filter { x: Int -> x % 2 == 0 }

    val evens = even.apply(listOf(1, 2, 3, 4).stream()).toList()

    println(evens)

    val s = sumUsingLoop().apply(listOf(1.0, 2.0, 3.0, 4.0).stream()).toList()

    println(s)

    val take5 = take<Int>(5).apply(listOf(1, 2, 3, 4, 5, 6, 7, 8).stream()).toList()

    println(take5)

    val drop5 = drop<Int>(5).apply(listOf(1, 2, 3, 4, 5, 6, 7, 8).stream()).toList()

    println(drop5)

    val takeWhile = takeWhile { x : Int ->
        x < 5
    }

    val tws = takeWhile.apply(listOf(1, 2, 3, 4, 5, 6, 4, 8, 9, 10).stream()).toList()

    println(tws)

    val dropWhile = dropWhile { x: Int -> x < 3 }

    val dws = dropWhile.apply(listOf(1, 2, 3, 4, 5, 6, 4, 8, 9, 10).stream()).toList()
    val dwsWithIdx = dropWhile.zipWithIndex().apply(listOf(1, 2, 3, 4, 5, 6, 4, 8, 9, 10).stream()).toList()

    println(dws)
    println(dwsWithIdx)

    val exists = exists<Int> { it < 5 }

    val existsSeq = exists.apply(listOf(1, 2, 3, 4, 5, 6, 4, 8, 9, 10).stream()).toList()

    println(existsSeq)

    val count = countUsingLoop<Int>().apply(listOf(1, 2, 3, 4, 5, 6, 4, 8, 9, 10).stream()).toList()

    println(count)

    val mean = meanUsingLoop().apply(listOf(1.0, 5.0, 10.0, 15.0).stream()).toList()

    println(mean)

    val meanAfterSum = sumUsingLoop().pipe(meanUsingLoop())

    val res = meanAfterSum.apply(listOf(1.0, 5.0, 10.0, 15.0).stream()).toList()

    println(res)

    val takeAppend = take<Int>(3).append { take(5) }

    val tas = takeAppend.apply(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).stream()).toList()

    assert(tas == listOf(1,2,3,4,5,6,7,8))

    val id = id<Int>()

    val ids = id.apply(listOf(1,2,3,4,5).stream()).toList()

    println(ids)
}
