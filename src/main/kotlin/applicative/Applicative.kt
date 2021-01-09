package applicative

import arrow.Kind
import arrow.core.curry
import monad.Functor

interface Applicative<F> : Functor<F> {
    fun <A> unit(a: () -> A): Kind<F, A>
    fun <A, B, C> map2(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C>

    override fun <A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B> = map2(this, unit {  }) { a1, _ -> f(a1) }

    fun <A, B> traverse(aSeq: List<A>, f: (A) -> Kind<F, B>): Kind<F, List<B>> =
        aSeq.foldRight(unit { listOf<B>() }) { a, acc ->
            map2(acc, f(a)) { t1, t2 ->
                t1.toMutableList().also { it.add(t2) }
            }
        }

    fun <A> sequence(fas: List<Kind<F, A>>): Kind<F, List<A>> = traverse(fas) { it }

    fun <A> replicateM(n: Int, fa: Kind<F, A>): Kind<F, List<A>> = fa.map { a -> (0 until n).map { a } }

    fun <A, B> product(fa: Kind<F, A>, fb : Kind<F, B>): Kind<F, Pair<A, B>> = map2(fa, fb) { a, b -> a to b }

    fun <A, B> applyK(fab: Kind<F, (A) -> B>, fa: Kind<F, A>): Kind<F, B> = map2(fab, fa) { ab, a -> ab(a) }

    fun <A, B, C> map2UsingApply(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> = applyK(applyK(unit { f.curry() }, fa), fb)

    fun <A, B> Kind<F, A>.mapUsingApply(f: (A) -> B): Kind<F, B> = applyK(unit { f }, this)

    fun <A, B, C, D> map3(fa: Kind<F, A>, fb: Kind<F, B>, fc: Kind<F, C>, f: (A, B, C) -> D): Kind<F, D>
            = applyK(map2(fa, fb) { a, b -> f.curryLast()(a, b) }, fc)

    fun <A, B, C, D, E> map4(fa: Kind<F, A>, fb: Kind<F, B>, fc: Kind<F, C>, fd: Kind<F, D>, f: (A, B, C, D) -> E): Kind<F, E>
            = applyK(map3(fa, fb, fc) { a, b, c -> f.curryLast()(a, b, c) }, fd)
}

fun <A, B, C, D, E> ((A, B, C, D) -> E).curryLast(): (A, B, C) -> (D) -> E =
    { a, b, c ->
        { d ->
            this(a, b, c, d)
        }
    }

fun <A, B, C, D> ((A, B, C) -> D).curryLast(): (A, B) -> (C) -> D =
    { a, b ->
        { c ->
            this(a, b, c)
        }
    }

fun <A, B, C, D> ((A, B, C) -> D).curry(): (A) -> (B) -> (C) -> D =
    { a ->
        { b ->
            { c ->
                this(a, b, c)
            }
        }
    }