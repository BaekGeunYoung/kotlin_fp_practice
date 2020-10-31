package paralellism

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

typealias Par<A> = (ExecutorService) -> Future<A>

fun <A> unit(a: A): Par<A> = { _ -> UnitFuture(a) }
fun <A> fork(a: () -> Par<A>): Par<A> = { es ->
    es.submit(Callable { a()(es).get() })
}

fun <A> lazyUnit(a: () -> A): Par<A> = fork { unit(a()) }
fun <A> run(s: ExecutorService, a: Par<A>): Future<A> = a(s)
fun <A, B, C> map2(par1: Par<A>, par2: Par<B>, f: (A, B) -> C): Par<C> = { es ->
    val af = par1(es)
    val bf = par2(es)
    UnitFuture(f(af.get(), bf.get()))
}

class UnitFuture<A>(
        val a : A
): Future<A> {
    override fun isDone(): Boolean = true

    override fun get(): A = a

    override fun get(timeout: Long, unit: TimeUnit): A = a

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

    override fun isCancelled(): Boolean = false
}

fun sum3(ints: List<Int>): Par<Int> {
    return if(ints.size == 1) unit(ints[0])
    else if(ints.isEmpty()) unit(0)
    else {
        val l = ints.subList(0, ints.size / 2)
        val r = ints.subList(ints.size / 2 + 1 , ints.size)

        map2(sum3(l), sum3(r)) { l_it, r_it ->
            l_it + r_it
        }
    }
}

fun <A, B> asyncF(f: (A) -> B): (A) -> Par<B> =
        { a ->
            lazyUnit { f(a) }
        }