package lol.unsession.features.application

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lol.unsession.features.db.Repository
import lol.unsession.features.security.Access
import lol.unsession.features.security.Roles
import lol.unsession.features.security.getUserDataFromToken
import lol.unsession.features.security.verify
import lol.unsession.models.Paging
import java.io.File
import kotlin.system.exitProcess

fun Application.configureAdminRouting() {
    routing {
        route("/admin") {
            route("/users") {
                get("/get") {
                    verify(Access.Users)
                    val paging = Paging.from(call)
                    call.respond(Repository.Users.getUsers(paging))
                }
                get("/ban") {
                    verify(Access.Users, Access.UsersBlocking)
                    val id = call.request.queryParameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    val reason =
                        call.request.queryParameters["reason"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val until = call.request.queryParameters["until"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    Repository.Users.banUser(
                        id = id,
                        reason = reason,
                        until = until
                    )
                    call.respond(HttpStatusCode.Created)
                }
                get("/unban") {
                    verify(Access.Users, Access.UsersBlocking)
                    val id = call.request.queryParameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    Repository.Users.banUser(
                        id = id,
                        reason = "",
                        until = 0
                    )
                    call.respond(HttpStatusCode.Created)
                }
                get("/delete") {
                    verify(Access.Users, Access.UsersRemoving)
                    logger.warn("WARNING: Used danger method DELETEUSER by user: ${call.getUserDataFromToken().id}")
                    val id = call.request.queryParameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    if (Repository.Users.removeUser(id)) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotModified)
                    }
                }
                get("/setRole") {
                    verify(Access.Users, Access.UsersRolesChanging)
                    val id = call.request.queryParameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    val role =
                        call.request.queryParameters["role"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    var newRole = Roles.Banned
                    val myRole = Roles.valueOf(Repository.Users.getUser(call.getUserDataFromToken().id)?.roleName!!)
                    try {
                        newRole = Roles.valueOf(role)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid role")
                    }
                    if (newRole == Roles.Superuser) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            "Only via direct database access. This incident will be reported."
                        )
                    }
                    if (myRole.ordinal < newRole.ordinal) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            "You can't set role higher than yours ${myRole.name}(${myRole.ordinal})"
                        )
                    }
                    if (Repository.Users.setRole(id, role = newRole)) {
                        logger.debug("Set role for user $id to $role")
                        call.respond(HttpStatusCode.OK)
                    } else {
                        logger.debug("Failed to set role for user $id to $role")
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
            route("/reviews") {
                get("/delete") {
                    verify(Access.Teachers, Access.TeachersReviewing)
                    val id = call.request.queryParameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    if (Repository.Reviews.removeReview(id)) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotModified)
                    }
                }
            }
            route("/server") {
                get("/shutdown") {
                    verify(Access.SS)
                    repeat(3) {
                        logger.error("WARNING: Used danger method SHUTDOWN by user: ${call.getUserDataFromToken().id}")
                    }
                    call.respond(HttpStatusCode.OK)
                    exitProcess(1488)
                }
                get("/dropDatabase") {
                    verify(Access.SS)
                    repeat(3) {
                        logger.error("WARNING: Used danger method DROPDATABASE by user: ${call.getUserDataFromToken().id}")
                    }
                    Repository.Global.dropDatabase()
                    call.respond(HttpStatusCode.OK)
                }
                get("/log") {
                    call.respondText {
                        File("./log").readText()
                    }
                }
            }
        }
    }
}