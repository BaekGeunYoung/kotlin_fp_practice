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
                    recv(Optional.empty()).apply(s)
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


fun <I> dropWhile(f: (I) -> Boolean): Process<I, I> =
    Await {
        if (it.isPresent) {
            if (f(it.get())) Await { o ->
                if (o.isPresent) dropWhile(f)
                else Halt()
            }
            else Emit(it.get(), drop(0))
        }
        else Halt()
    }

fun main() {
    val even = filter { x: Int -> x % 2 == 0 }

    val evens = even.apply(listOf(1, 2, 3, 4).stream()).toList()

    println(evens)

    val s = sum().apply(listOf(1.0, 2.0, 3.0, 4.0).stream()).toList()

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

    val dropWhile = dropWhile { x: Int -> x < 5 }

    val dws = dropWhile.apply(listOf(1, 2, 3, 4, 5, 6, 4, 8, 9, 10).stream()).toList()

    println(dws)
}
