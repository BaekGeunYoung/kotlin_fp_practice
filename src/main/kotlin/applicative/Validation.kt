package applicative

import arrow.Kind
import arrow.Kind2
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

sealed class Validation<out E, out A> : ValidationOf<E, A>

class Failure<E>(val head: E, val tail: Vector<E> = Vector()) : Validation<E, Nothing>() {
    override fun toString(): String = Vector(listOf(head)).apply { addAll(tail) }.toString()
}

class Success<A>(val a: A) : Validation<Nothing, A>() {
    override fun toString(): String = a.toString()
}


class ForValidationK private constructor() { companion object }

typealias ValidationOf<A, B> = Kind2<ForValidationK, A, B>
typealias ValidationPartialOf<A> = Kind<ForValidationK, A>

fun <A, B> ValidationOf<A, B>.fix(): Validation<A, B> = this as Validation<A, B>


interface ValidationApplicative<E> : Applicative<ValidationPartialOf<E>> {
    override fun <A> unit(a: () -> A): Validation<E, A> = Success(a())

    override fun <A, B, C> map2(fa: ValidationOf<E, A>, fb: ValidationOf<E, B>, f: (A, B) -> C): Validation<E, C> =
        fa.fix().let { va ->
            fb.fix().let { vb ->
                when (va) {
                    is Success -> when (vb) {
                        is Success -> Success(f(va.a, vb.a))
                        is Failure -> vb
                    }
                    is Failure -> when (vb) {
                        is Success -> va
                        is Failure -> Failure(va.head, va.tail.apply {
                            this.add(vb.head)
                            this.addAll(vb.tail)
                        })
                    }
                }
            }
        }
}

fun validName(name: String): Validation<String, String> =
    if (name != "") Success(name)
    else Failure("name cannot be empty")

fun validDate(date: String): Validation<String, LocalDate> =
    try {
        Success(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    } catch (e: DateTimeParseException) {
        Failure("date must be in the form of yyyy-MM-dd")
    }

fun validPhone(phoneNumber: String): Validation<String, String> =
    if (phoneNumber.matches("\\d{11}".toRegex())) Success(phoneNumber)
    else Failure("Phone number must be 11 digits")

val stringValidationApplicative = object : ValidationApplicative<String> {}

fun validWebForm(name: String, date: String, phone: String): Validation<String, Triple<String, LocalDate, String>> =
    stringValidationApplicative.map3(validName(name), validDate(date), validPhone(phone)) { a, b, c ->
            Triple(a, b, c)
    }.fix()

fun main() {
    val name = "a"
    val date = "2020-11-15"
    val phone = "01012341234"

    val result = validWebForm(name, date, phone)

    println(result)
}
