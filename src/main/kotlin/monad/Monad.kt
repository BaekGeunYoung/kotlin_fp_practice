package monad

import arrow.Kind
import java.util.*

interface Monad<F>: Functor<F> {
    fun <A> unit(a: () -> A): Kind<F, A>
    fun <A, B> Kind<F, A>.flatMap(f: (A) -> Kind<F, B>): Kind<F, B>

    override fun <A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B> = this.flatMap { unit { f(it) } }

    fun <A, B, C> map2(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> = fa.flatMap { a ->
        fb.map { b -> f(a, b) }
    }

    fun <A> sequence(lma: List<Kind<F, A>>): Kind<F, List<A>> =
        lma.foldRight(unit { listOf<A>() }) { a, b ->
            map2(a, b) { a2, b2 ->
                b2.toMutableList().apply { add(0, a2) }
            }
        }

    fun <A, B> traverse(la: List<A>, f: (A) -> Kind<F, B>): Kind<F, List<B>> = sequence(la.map(f))

    fun <A> replicateM(n: Int, ma: Kind<F, A>): Kind<F, List<A>> =
        ma.map { a -> (0 until n).map { a } }

    fun <A, B> product(ma: Kind<F, A>, mb: Kind<F, B>): Kind<F, Pair<A, B>> = map2(ma, mb) { a, b -> a to b }

    fun <A> filterM(ms: List<A>, f: (A) -> Kind<F, Boolean>): Kind<F, List<A>> =
        this.product(traverse(ms) { unit { it } }, traverse(ms, f)).map {
            it.first.filterIndexed { index, _ ->
                it.second[index]
            }
        }

    fun <A, B, C> compose(f: (A) -> Kind<F, B>, g: (B) -> Kind<F, C>): (A) -> Kind<F, C> = { f(it).flatMap(g) }

    fun <A, B> Kind<F, A>.flatMapUsingCompose(f: (A) -> Kind<F, B>): Kind<F, B> {
        val g : (Unit) -> Kind<F, A> = { this }
        return compose(g, f)(Unit)
    }
    /**
     * 결합법칙의 flatMap을 이용한 표현과 compose를 이용한 표현은 동치이다.
     *
     * 증명 :
     * compose(compose(f, g), h)(x) == compose(f, compose(g, h)(x)
     * => (a => compose(f, g)(a).flatMap(h)(x) == (a => f(a).flatMap(compose(g, h)))(x)
     * => (a => f(a).flatMap(g).flatMap(h))(x) == (a => f(a).flatMap(b => g(b).flatMap(h)))(x)
     * => f(x).flatMap(g).flatMap(h) == f(x).flatMap(b => g(b).flatMap(h))
     * => X.flatMap(g).flatMap(h) == X.flatMap(a => g(a).flatMap(h))
     */

    /**
     * 항등법칙의 flatMap을 이용한 표현과 compose를 이용한 표현은 동치이다.
     *
     * 증명 :
     * compose(f, unit) == f
     * => (a => f(a).flatmap(unit)) == (a => f(a))
     * => x.flatMap(unit) == x
     * &
     * compose(unit, f) == f
     * => (a => unit(a).flatMap(f)) == (a => f(a))
     */

    fun <A> join(mma: Kind<F, Kind<F, A>>): Kind<F, A> = mma.flatMap { it }

    /**
     * 결합법칙의 join, map을 이용한 표현
     *
     * compose(f, compose(g, h)) == compose(compose(f, g), h)
     * (a => join(f(a).map(compose(g, h)))) == (a => join(compose(f, g)(a).map(h)))
     * (a => join(f(a).map(b => join(g(b).map(h)))) == (a => join(b => join(f(b).map(g))(a).map(h)))
     * => join(f(a).map(b => join(g(b).map(h))) == join(b => join(f(b).map(g))(a).map(h))
     *
     *
     * x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
     * join(x.map(f)).flatMap(g) == join(x.map(a => f(a).flatMap(g))
     * join(join(x.map(f)).map(g)) == join(x.map(a => join(f(a).map(g))))
     *
     * 항등법칙의 join, map, unit을 이용한 표현
     * x.flatMap(unit) == x
     * => join(x.map(unit)) == x
     * &
     * unit(x).flatMap(f) == f(x)
     * => join(unit(x).map(f)) == f(x)
     */

    fun <A, B> Kind<F, A>.flatMapUsingJoin(f: (A) -> Kind<F, B>): Kind<F, B> = join(this.map(f))

    fun <A, B, C> composeUsingJoin(f: (A) -> Kind<F, B>, g: (B) -> Kind<F, C>): (A) -> Kind<F, C> = {
        join(f(it).map(g))
    }
}

object ListMonad : Monad<ForListK> {
    override fun <A> unit(a: () -> A): Kind<ForListK, A> = ListK(listOf(a()))

    override fun <A, B> Kind<ForListK, A>.flatMap(f: (A) -> Kind<ForListK, B>): Kind<ForListK, B> =
        this.fix().flatMap { f(it).fix() }
}

object OptionMonad : Monad<ForOptionK> {
    override fun <A> unit(a: () -> A): Kind<ForOptionK, A> = OptionK(Optional.of(a()))

    override fun <A, B> Kind<ForOptionK, A>.flatMap(f: (A) -> Kind<ForOptionK, B>): Kind<ForOptionK, B> = OptionK(this.fix().option.flatMap { f(it).fix().option })
}

object IdMonad : Monad<ForIdK> {
    override fun <A> unit(a: () -> A): Kind<ForIdK, A> = IdK(a())

    override fun <A, B> Kind<ForIdK, A>.flatMap(f: (A) -> Kind<ForIdK, B>): Kind<ForIdK, B> = this.fix().flatMap { f(it).fix() }
}

fun main() {
    val f: (Int) -> OptionK<Boolean> = { i : Int ->
        if(i > 10) OptionK(Optional.of(false))
        else OptionK(Optional.of(true))
    }

    val list = listOf(8, 9, 10, 11, 12)

    val listOption = OptionMonad.traverse(list) {
        OptionK(Optional.of(it * 10))
    }

    val optionFiltered = OptionMonad.filterM(list, f)

    println(listOption.fix().option)
    println(optionFiltered.fix().option)

    val unit: (Int) -> Kind<ForOptionK, Int> = { OptionMonad.unit { it } }

    // 다음 식은 항등식이다.
    OptionMonad.compose(unit, f) == f

    val res = IdK("hello ").flatMap { a ->
        IdK("world").flatMap { b ->
            IdK(a + b)
        }
    }
}