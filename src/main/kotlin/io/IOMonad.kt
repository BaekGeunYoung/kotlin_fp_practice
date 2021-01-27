package io

import arrow.fx.*
import arrow.fx.extensions.fx

sealed class TailRec<A> {
    fun <B> map(f: (A) -> B): TailRec<B> = this.flatMap { Return(f(it)) }
    fun <B> flatMap(f: (A) -> TailRec<B>): TailRec<B> = FlatMap(this, f)

    companion object {
        fun <A, B> forever(a: TailRec<A>): TailRec<B> {
            val t: () -> TailRec<B> = { forever(a) }
            return a.flatMap { t() }
        }
    }
}

class Return<A>(val a: A) : TailRec<A>()
class Suspend<A>(val resume: () -> A): TailRec<A>()
class FlatMap<A, B>(val sub: TailRec<A>, val k: (A) -> TailRec<B>): TailRec<B>()

@Suppress("UNCHECKED_CAST")
tailrec fun <A> run(io: TailRec<A>): A = when(io) {
    is Return<A> -> io.a
    is Suspend<A> -> io.resume()
    is FlatMap<*, A> -> {
        val k = io.k as (Any?) -> TailRec<A>

        when(io.sub) {
            is Return -> run(k(io.sub.a))
            is Suspend -> run(k(io.sub.resume()))
            is FlatMap<*, *> -> {
                run(io.sub.sub.flatMap {
                    val subk = io.sub.k as (Any?) -> TailRec<A>
                    subk(it).flatMap(k)
                })
            }
        }
    }
}

fun fToC(f: Double): Double = (f - 32) * (5.0 / 9.0)

val printLine: (String) -> TailRec<Unit> = {
    Suspend { println(it) }
}

val printForever = TailRec.forever<Unit, Unit>(printLine("hello"))

val readLine: () -> TailRec<String> = {
    Suspend { kotlin.io.readLine()!! }
}

val converter: TailRec<Unit> =
    printLine("Enter a temperature in degrees Fahrenheit: ")
        .flatMap { readLine().map { it.toDouble() } }
        .flatMap { printLine(fToC(it).toString()) }

val printLineIO: (String) -> IO<Unit> = {
    IO.invoke { println(it) }
}

val readLineIO: () -> IO<String> = {
    IO.invoke { kotlin.io.readLine()!! }
}

val converterIO: IO<Unit> = IO.fx {
    val _1 = printLineIO("Enter a temperature in degrees Fahrenheit: ").bind()
    val f = readLineIO().map { it.toDouble() }.bind()
    val result = printLineIO(fToC(f).toString()).bind()
}

val converterIOFlatMap =
    printLineIO("Enter a temperature in degrees Fahrenheit: ")
        .flatMap { readLineIO().map { it.toDouble() } }
        .flatMap { printLineIO(fToC(it).toString()) }


fun main() {
//    converterIOFlatMap.fix().unsafeRunSync()
    converterIO.fix().unsafeRunSync()
}
