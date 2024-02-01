package lol.unsession.plugins

import freemarker.cache.ClassTemplateLoader
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import lol.unsession.db.Repository
import lol.unsession.db.models.Paging
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.models.client.Review
import lol.unsession.security.permissions.Access.*
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import lol.unsession.utils.getLogger

val logger = getLogger("Routing")

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(PartialContent) {
        maxRangeCount = 10
    }
    install(AutoHeadResponse)
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    routing {
        get("/ping") {
            call.respondText("pong")
        }
        get("/") {
            val fileContent = Application::class.java.getResource("/static/zhdun")!!.readText(Charsets.UTF_8)
            call.respondText(fileContent)
        }
//        get("/td") {
//            generateTestData {
//                call.respond(it)
//            }
//        }
        route("/v1") {
            route("users") {
                post("/login") {
                    val loginData = call.receive<User.UserLoginData>()

                    val user = Repository.Users.getUser(loginData.email)
                    if (user == null) call.respond(HttpStatusCode.Unauthorized)
                    if (user!!.isBanned) call.respond(HttpStatusCode.Unauthorized)

                    val storedLoginData = user.userLoginData
                    if (storedLoginData == null) call.respond(HttpStatusCode.Unauthorized)

                    if (!Crypto.checkPassword(
                            inputPassword = loginData.password,
                            salt = storedLoginData!!.salt!!,
                            storedHash = storedLoginData.password
                        )
                    ) call.respond(HttpStatusCode.Unauthorized)

                    val token = createToken(user)

                    call.respond(LoginResponse(token, user))
                }
                post("/register") {
                    try {
                        val loginData = call.receive<User.UserLoginData>()
                        if (!loginData.validate()) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid login data")
                        }
                        Repository.Users.tryRegisterUser(loginData, call.request.origin.remoteAddress, onSuccess = {
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
            }
            authenticate("user-auth") {
                route("/teachers") {
                    post("/create") {
                        verify(Teachers, TeachersAdding)
                        val newTeacher =
                            call.receiveNullable<TeacherDto>()
                                ?: return@post call.respond(HttpStatusCode.BadRequest)
                        Repository.Teachers.addTeacher(newTeacher)
                            ?: return@post call.respond(HttpStatusCode.NotModified)
                        call.respond(HttpStatusCode.OK)
                    }
                    get("/get") {
                        verify(Teachers)
                        val params = Paging.from(call)
                        val teachers = Repository.Teachers.getTeachers(params)
                        call.respond(teachers)
                    }
                    get("/search") {
                        verify(Teachers)
                        val params = Paging.from(call)
                        val prompt = call.queryParameters["prompt"] ?: ""
                        if (prompt == "" || prompt.length < 3) {
                            call.respond(HttpStatusCode.BadRequest, "No prompt specified or len < 3")
                        }
                        val teachers = Repository.Teachers.searchTeachers(prompt, params)
                        call.respond(teachers)
                    }
                    get("/getById") {
                        verify(Teachers)
                        val id = call.request.queryParameters["id"]?.toIntOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, "No id specified")
                        }
                        val teacher = Repository.Teachers.getTeacher(id!!)
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
                        call.receiveNullable<Review>() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val review = Repository.Reviews.addReview(newReview)
                    if (review != null) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotModified)
                    }
                }
                get("/get") {
                    verify(TeachersReviewing)
                    val params = Paging.from(call)
                    val reviews = Repository.Reviews.getReviews(params).onEach { it.user.clearPersonalData() }
                    call.respond(reviews)
                }
                get("/getById") {
                    verify(Teachers)
                    val id = call.request.queryParameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "No id specified")
                    }
                    val review = Repository.Reviews.getReview(id!!).apply { this?.user?.clearPersonalData() }
                    if (review == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respond(review)
                    }
                }
                get("/getByTeacher") {
                    verify(Teachers)
                    val params = Paging.from(call)
                    val teacherId = call.request.queryParameters["teacherId"]?.toIntOrNull()
                    val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                    if (teacherId == null || userId == null) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                    val reviews =
                        Repository.Reviews.getReviewsByTeacher(teacherId!!, params)
                            .onEach { it.user.clearPersonalData() }
                    call.respond(reviews)
                }
                get("/getByUser") {
                    verify(Teachers)
                    val params = Paging.from(call)
                    val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                    if (userId != null) {
                        val reviews =
                            Repository.Reviews.getReviewsByUser(userId, params).onEach { it.user.clearPersonalData() }
                        call.respond(reviews)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
