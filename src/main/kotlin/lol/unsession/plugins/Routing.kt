package lol.unsession.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.repo.TeachersReviewsRepository
import lol.unsession.db.repo.UsersRepositoryImpl
import lol.unsession.security.permissions.Access
import lol.unsession.security.permissions.Access.*
import lol.unsession.security.user.User
import lol.unsession.utils.getLogger
import org.koin.ktor.ext.get

val logger = getLogger("Routing")

fun Application.configureRouting() {
    val usersRepo = UsersRepositoryImpl()
    val teachersRepo = TeachersReviewsRepository()
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("It works!")
        }
        post("/register") {
            val loginData = call.receive<User.UserLoginData>()
            usersRepo.tryRegisterUser(loginData, call.request.origin.remoteHost, onSuccess = {
                val token = createToken(it)
                logger.info("Registered user ${loginData.username}; ${call.request.origin.remoteHost}")
                call.respond(hashMapOf("token" to token, "user" to it))
            }, usernameExists = {
                logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-username-exists]")
                call.respond(HttpStatusCode.Conflict)
            }, userExists = {
                logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-id-exists]")
                call.respond(HttpStatusCode.Conflict)
            }, onFailure = {
                logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost}")
            })
        }
        authenticate("user-auth") {
            route("/teachers") {
                post("/create") {
                    verify(TeachersAdding)
                    val newTeacher =
                        call.receiveNullable<TeacherDto>() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val teacher = teachersRepo.addTeacher(newTeacher)
                    if (teacher) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotModified)
                    }
                }
                get("/get") {
                    verify(Teachers)
                    val params = call.request.queryParameters
                    val page = params["page"]?.toIntOrNull() ?: -1
                    if (page == -1) {
                        call.respond(HttpStatusCode.BadRequest, "No page specified")
                    } else {
                        val teachers = teachersRepo.getTeachers(page)
                        call.respond(teachers)
                    }
                }
            }
            route("/reviews") {
                post("/create") {
                    verify(Teachers, TeachersReviewing)
                    val newReview =
                        call.receiveNullable<ReviewDto>() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val review = teachersRepo.addReview(newReview)
                    if (review) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotModified)
                    }
                }
                get("/getById") {
                    verify(Teachers)
                    val id = call.request.queryParameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "No id specified")
                    }
                    val review = teachersRepo.getReview(id!!)
                    if (review == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respond(review)
                    }
                }
                get("/getByTeacher") {
                    verify(Teachers)
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: -1
                    if (page == -1) {
                        call.respond(HttpStatusCode.BadRequest, "No page specified")
                    }
                    val teacherId = call.request.queryParameters["teacherId"]?.toIntOrNull()
                    val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                    if (teacherId == null || userId == null) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                    val reviews = teachersRepo.getReviewsByTeacher(teacherId!!, page)
                    call.respond(reviews)
                }
                get("/getByUser") {
                    verify(Teachers)
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: -1
                    if (page == -1) {
                        call.respond(HttpStatusCode.BadRequest, "No page specified")
                    }
                    val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                    if (userId != null) {
                        val reviews = teachersRepo.getReviewsByUser(userId, page)
                        call.respond(reviews)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
