package applicative

import arrow.Kind
import arrow.core.curry
import arrow.core.some
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

    fun <A, B, C> assoc(p: Pair<A, Pair<B, C>>): Pair<Pair<A, B>, C> = (p.first to p.second.first) to p.second.second

    fun <A, B, C> map2UsingProduct(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> = product(fa, fb).map { f(it.first, it.second) }

    /**
     * applicative 항등 법칙
     * applicative trait의 map2 연산자에는 연산의 대상이 되는 인자가 2개이므로 왼쪽 항등 법칙과 오른쪽 항등 법칙이 존재함.
     * 왼쪽 항등 법칙 : map2(unit(), fa)((_, a) => a) == fa
     * 오른쪽 항등 법칙 : map2(fa, unit())((a, _) => a) == fa
     *
     * applicative 결합 법칙
     * 결합법칙은 product를 이용해 표현할 수 있다. (product는 map2를 이용해 두 효과를 하나로 묶는 함수이므로)
     * product(product(fa, fb), fc) == product(fa, product(fb, fc))
     * 그런데 반환되는 자료형식이 다르므로, assoc 함수를 우변에 적용함으로써 결합 법칙을 완성할 수 있다.
     * ==> product(product(fa, fb), fc) == product(fa, product(fb, fc)).map(assoc)
     *
     * applicative 자연성 법칙
     * map2는 특정 함수에 따라 값을 변환하고 두 값을 하나로 결합하는 역할을 한다.
     * 이 map2에서 일어나는 변환은 사실 map2 바깥에서 적용할 수도 있다.
     * 즉, 변환을 map2로 값을 결합하기 이전에 적용한 것과 결합한 이후에 적용한 것은 같다.
     * ==> map2(a, b)(productF(f,g)) == product(map(a)(f), map(b)(g))
     *
     */
    fun <I, O, I2, O2> productF(f: (I) -> O, g: (I2) -> O2): (I, I2) -> Pair<O, O2> = { i, i2 -> f(i) to g(i2) }

    /**
     * 모든 Monad는 Applicative이다.
     * 1. 항등 법칙
     * 왼쪽
     * => map2(unit(), fa)((_, a) => a)
     * == unit().flatMap(_ => fa.map(a => a))
     * == unit().flatMap(_ => fa)
     * == fa (by monad isomorphism)
     * 오른쪽
     * => map2(fa, unit())((a, _) => a)
     * == fa.flatMap(a => unit().map(_ => a))
     * == fa.flatMap(a => unit(a))
     * == fa.flatMap(unit)
     * == fa (by monad isomorphism)
     *
     * 2. 결합 법칙
     * => product(product(fa, fb), fc)
     * == map2(map2(fa, fb)(a, b => (a, b)), fc)((a,b), c => (a, b), c)
     * == (fa.flatMap(a => fb.map(b => (a, b)))).flatMap((a,b) => fc.map(c => (a,b), c))
     * == fa
     *    .flatMap(a =>
     *      fb.flatMap(b => (a, b))
     *    )
     *    .flatMap(a, b =>
     *      fc.flatMap(c => unit((a,b),c))
     *    )
     *
     * == fa
     *    .flatMap(a =>
     *      fb.flatMap(b => unit(a,b))
     *        .flatMap(a, b => fc.flatMap(c => unit((a,b),c)))
     *    )
     *
     * == fa.flatMap(a =>
     *      fb.flatMap(b => (a,b).flatMap(a,b => fc.flatMap(c => unit((a,b),c))))
     *    )
     *
     * == fa.flatMap(a =>
     *      fb.flatMap(b =>
     *          unit(a to b).flatMap(a,b => fc.flatMap(c => unit((a to b) to c)))
     *      )
     *    )
     *
     * == fa.flatMap(a =>
     *      fb.flatMap(b =>
     *          fc.flatMap(c => unit((a to b) to c))
     *      )
     *    ) by Monad isomorphism ... 1
     *
     * &
     * product(fa, product(fb, fc)).map(assoc)
     * == map2(fa, map2(fb, fc)(b, c => (b,c)))(a,(b,c) => a, (b, c)).map(assoc)
     * == map2(fa, map2(fb, fc)(b, c => (b,c)))(a,(b,c) => a, (b, c)).flatMap(a,(b,c) => unit(assoc(a,(b,c))))
     * == map2(fa, map2(fb, fc)(b, c => (b,c)))(a,(b,c) => a, (b, c)).flatMap(a,(b,c) => unit((a,b),c)))
     * == fa.flatMap(a => fb.flatMap(b => fc.map(c => (b,c))).map(b,c => a, (b, c)).flatMap(a, (b,c) => unit((a,b),c))
     * == fa
     *   .flatMap(a =>
     *      fb.flatMap(b => fc.flatMap(c => unit(b,c)))
     *        .flatMap(b,c => unit(a, (b,c)))
     *   )
     *   .flatMap(a,(b,c) => unit((a,b),c))
     *
     * == fa
     *   .flatMap(a =>
     *     fb.flatMap(b => fc.flatMap(c => unit(b,c)))
     *       .flatMap(b,c => unit(a, (b,c)))
     *       .flatMap(a,(b,c) => unit((a,b),c))
     *   )
     *
     * == fa
     *   .flatMap(a =>
     *     fb.flatMap(b => fc.flatMap(c => unit(b,c)))
     *       .flatMap(b,c => unit(a, (b,c)).flatMap(a,(b,c) => unit((a,b),c)))
     *   )
     *
     * == fa
     *    .flatMap(a =>
     *      fb.flatMap(b => fc.flatMap(c => unit(b,c)))
     *        .flatMap(b,c => unit((a,b),c))
     *    )
     *
     * == fa.flatMap(a =>
     *      fb.flatMap(b =>
     *          fc.flatMap(c => unit(b to c)).flatMap(b,c => unit((a to b) to c))
     *      )
     *    )
     *
     * == fa.flatMap(a =>
     *      fb.flatMap(b =>
     *          fc.flatMap(c => unit((a to b) to c))
     *      )
     *    ) by Monad isomorphism ... 2
     *
     * 1 === 2 이므로 applicative의 결합법칙이 모든 Monad에도 성립한다.
     *
     */
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