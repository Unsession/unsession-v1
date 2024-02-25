import lol.unsession.parsers.SDOTest
import java.io.File

fun main() {
    val toParse =
        File("S:\\Projects\\Intelij\\unsessionserver\\src\\main\\kotlin\\lol\\unsession\\parsers\\sdo.html").readText(
            Charsets.UTF_8
        )

    println(SDOTest.parseSDOTest(toParse))
}
