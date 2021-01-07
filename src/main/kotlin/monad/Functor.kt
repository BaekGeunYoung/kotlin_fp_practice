package monad

import arrow.Kind
import arrow.core.Either
import arrow.higherkind
import java.util.*

interface Functor <F> {
    fun <A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B>
    fun <A, B> Kind<F, Pair<A, B>>.distribute(): Pair<Kind<F, A>, Kind<F, B>> = this.map { it.first } to this.map { it.second }
    fun <A, B> Either<Kind<F, A>, Kind<F, B>>.codistribute(): Kind<F, Either<A, B>> =
        if (this.isLeft()) (this as Either.Left<Kind<F, A>>).a.map { Either.Left(it) }
        else (this as Either.Right<Kind<F, B>>).b.map { Either.Right(it) }
}

class ForListK private constructor() { companion object }

@Suppress("UNCHECKED_CAST")
fun <A> Kind<ForListK, A>.fix(): ListK<A> = this as ListK<A>

@higherkind
data class ListK<T>(val list: List<T>) : Kind<ForListK, T>, List<T> by list {
    fun <B> map(f: (T) -> B): ListK<B> = ListK(this.list.map(f))
    fun <B> flatMap(f: (T) -> ListK<B>): ListK<B> = ListK(this.list.flatMap(f))
}

class ListFunctor : Functor<ForListK> {
    override fun <A, B> Kind<ForListK, A>.map(f: (A) -> B): Kind<ForListK, B> = fix().map(f)
}

class ForOptionK private constructor() { companion object }

fun <A> Kind<ForOptionK, A>.fix(): OptionK<A> = this as OptionK<A>

@higherkind
data class OptionK<T>(val option: Optional<T>) : Kind<ForOptionK, T> {
    fun <B> flatMap(f: (T) -> OptionK<B>): OptionK<B> = OptionK(this.option.flatMap { f(it).option })
}

class ForIdK private constructor() { companion object }

fun <A> Kind<ForIdK, A>.fix(): IdK<A> = this as IdK<A>

@higherkind
data class IdK<T>(val value: T) : Kind<ForIdK, T> {
    fun <B> flatMap(f: (T) -> IdK<B>): IdK<B> = f(value)
    fun <B> map(f: (T) -> B): IdK<B> = IdK(f(value))
}