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

fun <A, B> map(pa: Par<A>, f: (A) -> B): Par<B> = map2(pa, unit(Unit)) { a, _ -> f(a) }

fun sortPar(parList: Par<List<Int>>): Par<List<Int>> = map(parList) { list -> list.sorted() }

fun <A, B> parMap(ps: List<A>, f: (A) -> B): Par<List<B>> = fork {
    val fbs: List<Par<B>> = ps.map { asyncF(f)(it) }
    sequence(fbs)
}

fun <A> parFilter(aSeq: List<A>, f: (A) -> Boolean): Par<List<A>> = fork {
    val pars: List<Par<List<A>>> = aSeq.map(
            asyncF { a: A -> if (f(a)) listOf(a) else listOf() }
    )

    map(sequence(pars)) {
        it.flatten()
    }
}

fun <A> sequence(ps: List<Par<A>>): Par<List<A>> = { es ->
    val list: List<A> = ps.map { it(es).get() }
    UnitFuture(list)
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

fun test() {
    val f: (Int) -> String = { it.toString() }


}