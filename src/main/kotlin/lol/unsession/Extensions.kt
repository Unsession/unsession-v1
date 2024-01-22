package lol.unsession

import io.ktor.http.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

val HttpStatusCode.Companion.Teapot: HttpStatusCode
    get() = HttpStatusCode(418, "I'm a teapot")

object Utils {
    val now: Int
        get() = (Clock.System.now().epochSeconds).toInt()
    val nowMills: Long
        get() = (Clock.System.now().toEpochMilliseconds())
}

val mainDir = "${System.getProperty("user.dir")}\\src\\main\\kotlin\\lol\\unsession\\"

fun Table.findColumn(name: String): Column<*>? {
    return this.columns.find { it.name == name }
}