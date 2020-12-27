package testing

sealed class Result {
    abstract val isFalsified: Boolean
}

class Passed : Result() {
    override val isFalsified: Boolean = false
}

class Falsified(
    val failure: String,
    val successes: Int
) : Result() {
    override val isFalsified: Boolean = true
}