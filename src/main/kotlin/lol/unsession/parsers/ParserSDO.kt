import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import lol.unsession.db.models.SDOTest
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main() {
    val toParse =
        File("S:\\Projects\\Intelij\\unsessionserver\\src\\main\\kotlin\\lol\\unsession\\parsers\\sdo.html").readText(
            Charsets.UTF_8
        )

    println(SDOTest.parseSDOTest(toParse))
}
