package monoid

import arrow.Kind
import monoid.FoldableList.foldRight

interface Foldable<F> {
    fun <A, B> Kind<F, A>.foldRight(z: B, f: (A, B) -> B): B
    fun <A, B> Kind<F, A>.foldLeft(z: B, f: (A, B) -> B): B
    fun <A, B> Kind<F, A>.foldMap(f: (A) -> B, mb: Monoid<B>): B
    fun <A> Kind<F, A>.concatenate(m: Monoid<A>) = this.foldLeft(m.zero) { x, y -> m.op(x, y) }
}

class ForList private constructor()
typealias ListOf<A> = Kind<ForList, A>

@Suppress("UNCHECKED_CAST")
fun <A> ListOf<A>.fix(): List<A> = this as List<A>

object FoldableList : Foldable<ForList> {
    @Suppress("UNCHECKED_CAST")
    fun <A> of(vararg aSeq: A): ListOf<A> {
        val list = aSeq.toList()
        return list as ListOf<A>
    }

    override fun <A, B> ListOf<A>.foldRight(z: B, f: (A, B) -> B): B {
        val list = this.fix()
        var acc = z

        list.forEach {
            acc = f(it, acc)
        }

        return acc
    }

    override fun <A, B> ListOf<A>.foldLeft(z: B, f: (A, B) -> B): B {
        val list = this.fix().reversed()
        var acc = z

        list.forEach {
            acc = f(it, acc)
        }

        return acc
    }

    override fun <A, B> ListOf<A>.foldMap(f: (A) -> B, mb: Monoid<B>): B {
        val list = this.fix()
        var acc = mb.zero

        list.forEach {
            val b = f(it)
            acc = mb.op(acc, b)
        }

        return acc
    }
}

fun main() {
    val fList: ListOf<Int> = FoldableList.of(1, 2, 3)
    val test = fList.foldRight("") { x, y ->
        x.toString() + x.toString() + x.toString() + y
    }

    println(test)
}
