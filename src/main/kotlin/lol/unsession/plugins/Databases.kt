package lol.unsession.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import lol.unsession.db.UnsessionSchema
import org.jetbrains.exposed.sql.*

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/unsession",
        user = "postgres",
        driver = "org.postgresql.Driver",
        password = System.getenv("pgpass")
    )
    val schema = UnsessionSchema(database)
}
