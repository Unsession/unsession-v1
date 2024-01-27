package lol.unsession

import io.ktor.http.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnSet

val HttpStatusCode.Companion.Teapot: HttpStatusCode
    get() = HttpStatusCode(418, "I'm a teapot")

object Utils {
    val now: Int
        get() = (Clock.System.now().epochSeconds).toInt()
    val nowMills: Long
        get() = (Clock.System.now().toEpochMilliseconds())
}

val mainDir = "${System.getProperty("user.dir")}\\src\\main\\kotlin\\lol\\unsession\\"

fun ColumnSet.findColumn(name: String): Column<*>? {
    return this.columns.find { it.name == name }
}

fun <K, V> Map<K, V>.containsAnyKey(keys: Collection<K>): Boolean {
    return keys.any { this.containsKey(it) }
}

fun <K, V> Map<K, V>.containsAnyValue(vararg value: V): Boolean {
    return value.any { this.containsValue(it) }
}

fun <K, V> Map<K, V>.containsAnyKeyNotIn(keys: Collection<K>): Boolean {
    return keys.any { !this.containsKey(it) }
}

fun <K, V> Map<K, V>.containsAnyValueNotIn(vararg value: V): Boolean {
    return value.any { !this.containsValue(it) }
}

fun isDebug() = System.getenv("debugmode") == "true"