package lol.unsession.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import lol.unsession.Teapot
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.repo.TeachersReviewsRepositoryImpl
import lol.unsession.db.repo.UsersRepositoryImpl
import lol.unsession.db.repo.generateTestData
import lol.unsession.security.permissions.Access.*
import lol.unsession.security.user.User
import lol.unsession.utils.getLogger

val logger = getLogger("Routing")

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)

fun Application.configureRouting() {
    val usersRepo = UsersRepositoryImpl
    val teachersRepo = TeachersReviewsRepositoryImpl
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
            try {
                val loginData = call.receive<User.UserLoginData>()
                if (!loginData.validate()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid login data")
                }
                usersRepo.tryRegisterUser(loginData, call.request.origin.remoteHost, onSuccess = {
                    val token = createToken(it)
                    logger.info("Registered user ${loginData.username}; ${call.request.origin.remoteHost}; ${call.request.headers}")
                    call.respond(LoginResponse(token, it))
                }, usernameExists = {
                    logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-username-exists]")
                    call.respond(HttpStatusCode.Conflict)
                }, userExists = {
                    logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-id-exists]")
                    call.respond(HttpStatusCode.Conflict)
                }, onFailure = {
                    logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost}")
                })
            } catch (e: Exception) {
                logger.error(e)
            }
        }
        authenticate("user-auth") {
            get("/authtest") {
                call.respond(HttpStatusCode.OK, "Hello, world")
            }
        }
        route("/api") {
            route("/v1") {
                authenticate("user-auth") {
                    route("/teachers") {
                        post("/create") {
                            verify(TeachersAdding)
                            val newTeacher =
                                call.receiveNullable<TeacherDto>()
                                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                            teachersRepo.addTeacher(newTeacher) ?: return@post call.respond(HttpStatusCode.NotModified)
                            call.respond(HttpStatusCode.OK)
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
                        get("/getById") {
                            verify(Teachers)
                            val id = call.request.queryParameters["id"]?.toIntOrNull()
                            if (id == null) {
                                call.respond(HttpStatusCode.BadRequest, "No id specified")
                            }
                            val teacher = teachersRepo.getTeacher(id!!)
                            if (teacher == null) {
                                call.respond(HttpStatusCode.NotFound)
                            } else {
                                call.respond(teacher)
                            }
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
                    get("/get") {
                        verify(TeachersReviewing)
                        val params = call.request.queryParameters
                        val page = params["page"]?.toIntOrNull() ?: -1
                        if (page == -1) {
                            call.respond(HttpStatusCode.BadRequest, "No page specified")
                        } else {
                            val reviews = teachersRepo.getReviews(page)
                            call.respond(reviews)
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
                get("/test") {
                    logger.debug("/test call")
                    logger.debug("headers{}", call.request.headers)
                    call.respond(HttpStatusCode.OK, "auth succeed")
                }
            }
            get("/generateTestData") {
                generateTestData()
                call.respond(
                    HttpStatusCode.Teapot,
                    "Generated teachers and 25 users with 5 reviews each"
                )
            }
        }
    }
}
