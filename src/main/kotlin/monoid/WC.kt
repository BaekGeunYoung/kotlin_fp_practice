package monoid

sealed class WC

data class Stub(val chars: String): WC()

data class Part(val lStub: String, val words: Int, val rStub: String): WC()

val wcMonoid: Monoid<WC> = object : Monoid<WC> {
    override val zero: WC = Stub("")

    override fun op(a1: WC, a2: WC): WC {
        when (a1) {
            is Stub -> return when (a2) {
                is Stub -> {
                    Stub(a1.chars + a2.chars)
                }
                is Part -> {
                    Part(a1.chars + a2.lStub, a2.words, a2.rStub)
                }
            }

            is Part -> return when (a2) {
                is Stub -> {
                    Part(a1.lStub, a1.words, a1.rStub + a2.chars)
                }
                is Part -> {
                    val words = a1.words + a2.words
                    if (a1.rStub.isEmpty() && a2.lStub.isEmpty()) Part(a1.lStub, words, a2.rStub)
                    else Part(a1.lStub, words + 1, a2.rStub)
                }
            }
        }
    }
}

fun main() {
    val str = "a qwe     qwe     xzc dqwe asd    asd "

    println(getWordCount(str))
}

fun getWordCount(str: String): Int =
    countWords(str).let { wc ->
        when (wc) {
            wcMonoid.zero -> 0
            is Stub -> 1
            is Part -> {
                var result = wc.words

                if (wc.rStub.isNotEmpty()) result++
                if (wc.lStub.isNotEmpty()) result++

                result
            }
        }
    }

fun countWords(str: String): WC {
    if (str.isEmpty()) return wcMonoid.zero
    if (str.length == 1) {
        return if (str.isBlank()) Part("", 0, "")
        else Stub(str)
    }

    val left = countWords(str.substring(0, str.length / 2))
    val right = countWords(str.substring(str.length / 2, str.length))

    return wcMonoid.op(left, right)
}


/**
 * String에서의 WC로의 준동형사항 coundWords는 다음 법칙을 따른다.
 * wcMonoid.op(countWords(x), countWords(y)) == countWords(strConcatMonoid.op(x, y))
 */


/**
 * List<Char>를 String으로 변환하는 함수 f가 있다고 하자.
 * f는 준동형사상이므로 다음 법칙을 따른다.
 * f(charsConcatMonoid.op(x, y)) == strConcatMonoid(f(x), f(y))
 */
val f : (List<Char>) -> String = {
    it.foldRight("") { a, b ->
        a + b
    }
}

/**
 * f의 역연산 g 또한 준동형사상이다.
 * strConcatMonoid.op(g(x), g(y)) == g(charsConcatMonoid.op(x, y))
 */
val g : (String) -> List<Char> = {
    it.toCharArray().toList()
}

/**
 * 이때 f와 g는 strConcatMonoid와 charsConcatMonoid 사이의 동형사상이다.
 */

val charsConcatMonoid: Monoid<List<Char>> = object : Monoid<List<Char>> {
    override val zero: List<Char> = listOf()

    override fun op(a1: List<Char>, a2: List<Char>): List<Char> =
        a1.toMutableList().also {
            it.addAll(a2)
        }
}

val orMonoid = object : Monoid<Boolean> {
    override val zero: Boolean = false
    override fun op(a1: Boolean, a2: Boolean): Boolean = a1 || a2
}

val andMonoid = object : Monoid<Boolean> {
    override val zero: Boolean = true
    override fun op(a1: Boolean, a2: Boolean): Boolean = a1 && a2
}

val negate : (Boolean) -> Boolean = { !it }

/**
 * is negate a homomorphism between orMonoid and andMonoid?
 * -> orMonoid.op(negate(x), negate(y)) == negate(andMonoid(x, y))
 * -> !x || !y == !(x && y)
 * -> !(x || y) == !x && !y
 */
