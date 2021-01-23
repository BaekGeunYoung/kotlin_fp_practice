package monoid

import arrow.Kind
import monad.ForListK

interface Foldable<F> {
    fun <A, B> Kind<F, A>.foldRight(z: B, f: (A, B) -> B): B
    fun <A, B> Kind<F, A>.foldLeft(z: B, f: (A, B) -> B): B
    fun <A, B> Kind<F, A>.foldMap(f: (A) -> B, mb: Monoid<B>): B
    fun <A> Kind<F, A>.toList(): List<A> = this.foldRight(
        listOf(),
        { a, b -> b.toMutableList().apply { add(a) } }
    )
    fun <A> Kind<F, A>.concatenate(m: Monoid<A>) = this.foldLeft(m.zero) { x, y -> m.op(x, y) }

    // fold 연산으로 map을 구현할 수 없는 이유 (== Foldable이 Functor를 확장할 수 없는 이유)
    // map은 입력값의 구조를 그대로 유지하는 사상인 반면 fold는 입력값의 구조를 잃어버리는 사상이기 때문에, fold로 map을 구현할 수 없음.
    // 그렇다면 foldable하지만 functor는 아닌 형식으로 어떤게 있을까
}

class ListConcatMonoid<T> : Monoid<List<T>> {
    override val zero: List<T> = listOf()
    override fun op(a1: List<T>, a2: List<T>): List<T> =
        a1.toMutableList().apply { addAll(a2) }
}


typealias ListOf<A> = Kind<ForListK, A>

@Suppress("UNCHECKED_CAST")
fun <A> ListOf<A>.fix(): List<A> = this as List<A>

object ListFoldable : Foldable<ForListK> {
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

    override fun <A> Kind<ForListK, A>.toList(): List<A> {
        TODO("Not yet implemented")
    }
}

fun main() {
    val bagA = bagUsingMonoid(listOf("a", "b", "c", "d", "a"))
    println(bagA)
}

sealed class Tree<A>

class Leaf<A>(val value: A) : Tree<A>()
class Branch<A>(val left: Tree<A>, val right: Tree<A>) : Tree<A>()

object TreeFoldable {
    fun <A, B> Tree<A>.foldRight(z: B, f: (A, B) -> B): B =
        when (this) {
            is Leaf -> f(this.value, z)
            is Branch -> this.left.foldRight(this.right.foldRight(z, f), f)
        }

    fun <A, B> Tree<A>.foldLeft(z: B, f: (A, B) -> B): B =
        when (this) {
            is Leaf -> f(this.value, z)
            is Branch -> this.right.foldLeft(this.left.foldLeft(z, f), f)
        }

    fun <A, B> Tree<A>.foldMap(f: (A) -> B, mb: Monoid<B>): B =
        when (this) {
            is Leaf -> f(this.value)
            is Branch -> mb.op(this.left.foldMap(f, mb), this.right.foldMap(f, mb))
        }
}

sealed class Option<out A>

class Some<out A>(val value: A) : Option<A>()
object None: Option<Nothing>()

object OptionFoldable {
    fun <A, B> Option<A>.foldRight(z: B, f: (A, B) -> B): B =
        when (this) {
            is None -> z
            is Some -> f(this.value, z)
        }

    fun <A, B> Option<A>.foldLeft(z: B, f: (A, B) -> B): B =
        when (this) {
            is None -> z
            is Some -> f(this.value, z)
        }

    fun <A, B> Option<A>.foldMap(f: (A) -> B, mb: Monoid<B>): B =
        when (this) {
            is None -> mb.zero
            is Some -> f(this.value)
        }
}

fun <A, B> productMonoid(a: Monoid<A>, b: Monoid<B>): Monoid<Pair<A, B>> {
    return object : Monoid<Pair<A, B>> {
        override val zero: Pair<A, B> = a.zero to b.zero
        override fun op(a1: Pair<A, B>, a2: Pair<A, B>): Pair<A, B> =
            a.op(a1.first, a2.first) to b.op(a1.second, a2.second)
    }
}

fun <A, B> functionMonoid(b: Monoid<B>): Monoid<(A) -> B> {
    return object : Monoid<(A) -> B> {
        override val zero: (A) -> B = { b.zero }
        override fun op(a1: (A) -> B, a2: (A) -> B): (A) -> B = {
            b.op(a1(it), a2(it))
        }
    }
}

fun <A> bag(aSeq: List<A>): Map<A, Int> =
    aSeq.foldRight(mutableMapOf()) { a, b ->
        b.apply {
            if (b.containsKey(a)) {
                b[a] = b[a]!! + 1
            }
            else {
                b[a] = 1
            }
        }
    }

fun <K, V> mapMergeMonoid(v: Monoid<V>): Monoid<Map<K, V>> =
    object : Monoid<Map<K, V>> {
        override val zero: Map<K, V> = mapOf()
        override fun op(a1: Map<K, V>, a2: Map<K, V>): Map<K, V> =
            (a1.keys + a2.keys).fold(zero) { acc, k ->
                acc.toMutableMap().apply {
                    put(k, v.op(a1.getOrDefault(k, v.zero), a2.getOrDefault(k, v.zero)))
                }
            }
    }

fun <A> bagUsingMonoid(aSeq: List<A>): Map<A, Int> = foldMap(aSeq, mapMergeMonoid<A, Int>(intAddition)) { mapOf(it to 1) }
