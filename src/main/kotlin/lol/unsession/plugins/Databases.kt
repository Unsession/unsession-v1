package lol.unsession.plugins

import lol.unsession.db.UnsessionSchema
import org.jetbrains.exposed.sql.Database

fun configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/unsession",
        user = "vlad",
        driver = "org.postgresql.Driver",
        password = System.getenv("DB_PASSWORD")
    )
    val schema = UnsessionSchema(database)
}

fun configureDatabasesLocalhost() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/unsession",
        user = "postgres",
        driver = "org.postgresql.Driver",
        password = System.getenv("DB_PASSWORD")
    )
    val schema = UnsessionSchema(database)
}