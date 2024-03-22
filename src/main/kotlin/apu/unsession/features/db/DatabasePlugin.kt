package apu.unsession.features.db

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://${
            if (System.getenv("localhost") == "true") "51.250.13.148" else "localhost"
        }:5432/unsession",
        user = System.getenv("DB_USER"),
        driver = "org.postgresql.Driver",
        password = System.getenv("DB_PASSWORD")
    )
    val schema = UnsessionSchema(database)
}

fun configureDatabasesLocalhost() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/unsession",
        user = System.getenv("DB_USER"),
        driver = "org.postgresql.Driver",
        password = System.getenv("DB_PASSWORD")
    )
    val schema = UnsessionSchema(database)
}