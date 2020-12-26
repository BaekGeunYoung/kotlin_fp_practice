package testing

interface Prop {
    fun and(p: Prop): Prop = object : Prop {
        override fun check(): Boolean = this@Prop.check() && p.check()
    }

    fun check(): Boolean
}

class Prop1 : Prop {
    override fun check() = false
}

class Prop2 : Prop {
    override fun check(): Boolean = true
}

fun main() {
    val prop2 = Prop2()
    val prop4 = Prop2()

    println(prop2.and(prop4).check())
}
