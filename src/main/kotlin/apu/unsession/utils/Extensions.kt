package apu.unsession

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnSet
import java.io.FileNotFoundException
import java.net.URL

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

fun getResourceUri(path: String): URL {
    return Application::class.java.getResource(path)?: throw FileNotFoundException("Resource not found: $path")
}

fun randSelect(vararg values: String): String {
    return values.random()
}