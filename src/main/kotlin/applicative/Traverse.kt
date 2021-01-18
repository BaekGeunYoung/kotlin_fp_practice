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
