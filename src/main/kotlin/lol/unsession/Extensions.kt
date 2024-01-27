package lol.unsession

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.datetime.Clock
import lol.unsession.db.models.Config
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnSet

val gson = Gson()

val HttpStatusCode.Companion.Teapot: HttpStatusCode
    get() = HttpStatusCode(418, "I'm a teapot")

object Utils {
    val now: Int
        get() = (Clock.System.now().epochSeconds).toInt()
    val nowMills: Long
        get() = (Clock.System.now().toEpochMilliseconds())
}

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

fun Application.env(name: String): ApplicationConfigValue {
    return this.environment.config.property(name)
}

fun readConfigJson(): String {
    val configStream = Thread.currentThread().contextClassLoader.getResourceAsStream("config.json")
    return configStream!!.reader().readText()
}

fun getConfig(): Config {
    return gson.fromJson(readConfigJson(), Config::class.java)!!
}