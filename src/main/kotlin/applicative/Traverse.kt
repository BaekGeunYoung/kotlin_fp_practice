package applicative

import arrow.Kind
import arrow.core.identity
import monad.*
import monoid.Foldable
import monoid.ListFoldable
import monoid.Monoid
import java.util.*

interface Traverse<F>: Functor<F>, Foldable<F> {
    fun <G, A, B> Kind<F, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, Kind<F, B>>
    fun <G, A> Kind<F, Kind<G, A>>.sequence(AG: Applicative<G>): Kind<G, Kind<F, A>> = traverse(AG, ::identity)

    fun <S, A, B> Kind<F, A>.mapAccum(s: S, f: (A, S) -> Pair<B, S>): Pair<Kind<F, B>, S> = TODO()

    fun <A, B> Kind<F, A>.foldRightUsingMapAccum(z: B, f: (A, B) -> B): B =
        this.mapAccum(z) { a, s ->
            Unit to f(a, s)
        }.second

    fun <G, H, A, B> Kind<F, A>.fuse(GA: Applicative<G>, HA: Applicative<H>, f: (A) -> Kind<G, B>, g: (A) -> Kind<H, B>): Pair<Kind<G, Kind<F, B>>, Kind<H, Kind<F, B>>> =
        this.traverse(GA, f) to this.traverse(HA, g)
}

object ListTraverse : Traverse<ForListK>, Functor<ForListK> by ListFunctor, Foldable<ForListK> by ListFoldable {
    override fun <G, A, B> Kind<ForListK, A>.traverse(
        AP: Applicative<G>,
        f: (A) -> Kind<G, B>
    ): Kind<G, Kind<ForListK, B>> =
        this.foldRight(AP.unit { ListK(listOf<B>()) }) { a, acc ->
            AP.map2(acc, f(a)) { t1, t2 ->
                t1.list.toMutableList().add(t2)
                t1
            }
        }
}

object OptionTraverse : Traverse<ForOptionK> {
    override fun <G, A, B> Kind<ForOptionK, A>.traverse(
        AP: Applicative<G>,
        f: (A) -> Kind<G, B>
    ): Kind<G, Kind<ForOptionK, B>> = AP.run {
        if (this@traverse.fix().option.isPresent) {
            f(this@traverse.fix().option.get()).map {
                OptionK(Optional.of(it))
            }
        } else {
            unit { OptionK(Optional.empty()) }
        }
    }

    override fun <A, B> Kind<ForOptionK, A>.map(f: (A) -> B): Kind<ForOptionK, B> {
        TODO("Not yet implemented")
    }

    override fun <A, B> Kind<ForOptionK, A>.foldRight(z: B, f: (A, B) -> B): B {
        TODO("Not yet implemented")
    }

    override fun <A, B> Kind<ForOptionK, A>.foldLeft(z: B, f: (A, B) -> B): B {
        TODO("Not yet implemented")
    }

    override fun <A, B> Kind<ForOptionK, A>.foldMap(f: (A) -> B, mb: Monoid<B>): B {
        TODO("Not yet implemented")
    }
}
