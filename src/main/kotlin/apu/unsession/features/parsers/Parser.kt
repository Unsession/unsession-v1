package apu.unsession.features.parsers

abstract class Parser<T>(
    val input: String,
) {
    abstract fun parse(): T
}

//class ParserSDO : Parser {
//
//}