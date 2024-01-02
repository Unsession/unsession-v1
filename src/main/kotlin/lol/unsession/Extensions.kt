package lol.unsession

import io.ktor.http.*

fun Number.toBoolean(): Boolean = this.toInt() == 1

fun formatInt(number: Int, bitLength: Int): String {
    val decimalString = number.toString() // Преобразование числа в десятичную строку
    val paddedDecimalString = decimalString.padStart(bitLength, '0') // Добавление нулей слева до нужной длины

    return paddedDecimalString.map { it.toString().toInt() }.toString() // Преобразование строки в список чисел
}

//@OptIn(ExperimentalStdlibApi::class)
//private fun ByteArray.toHexString(): String {
//    return HexFormat.of().formatHex(this)
//}

val HttpStatusCode.Teapot: HttpStatusCode
    get() = HttpStatusCode(418, "I'm a teapot")