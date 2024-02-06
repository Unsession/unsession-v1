package lol.unsession.utils

data class Result(
    val success: Boolean,
    val message: String?
) {
    companion object {
        fun success(): Result {
            return Result(true, null)
        }
        fun error(message: String): Result {
            return Result(false, message)
        }
    }
}