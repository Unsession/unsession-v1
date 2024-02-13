package lol.unsession.plugins

import lol.unsession.db.UnsessionSchema
import org.jetbrains.exposed.sql.Database

fun configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/unsession?useUnicode=yes&characterEncoding=Windows-1251&useSSL=false&serverTimezone=UTC",
        user = System.getenv("pguser"),
        driver = "org.postgresql.Driver",
        password = System.getenv("pgpassword")
    )
    val schema = UnsessionSchema(database)
}
