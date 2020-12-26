package reactor

import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux

fun main() {
    var a = 0
    val chain = (0 until 1000).toFlux().map {
        Thread.sleep(10L)
        a++
    }

    chain.subscribe()
    println(a)
}


fun printHelloAndReturn(value: Int): Int {
    println("hello $value")
    return value
}
