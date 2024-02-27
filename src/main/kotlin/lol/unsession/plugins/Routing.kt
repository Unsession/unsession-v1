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
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import lol.unsession.Utils
import lol.unsession.ai.AiModule.Censor.allowed
import lol.unsession.db.Repository
import lol.unsession.db.Repository.HolyTestObject.generateTestData
import lol.unsession.db.models.Paging
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.getResourceUri
import lol.unsession.security.permissions.Access.*
import lol.unsession.security.permissions.Roles
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import lol.unsession.utils.getLogger
import java.io.File
import kotlin.system.exitProcess

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
        swaggerUI("swagger", "openapi/documentation.yaml")
        get("/ping") {
            call.respondText("pong")
        }
        get("/") {
            delay(30000)
            val fileContent = getResourceUri("static/zhdun").readText()
            call.respondText(fileContent)
        }
        get("/td") {
            generateTestData {
                call.respond(it)
            }
        }
        route("/v1") {
            route("/users") {
                post("/login") {
                    val loginData = call.receive<User.UserLoginData>()

                    val user = Repository.Users.getUser(loginData.email)
                    if (user == null) call.respond(HttpStatusCode.Unauthorized)
                    if (user!!.isBanned) call.respond(HttpStatusCode.Forbidden, user.banData!!)

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
                route("/reviews") {
                    post("/create") {
                        try {
                        verify(Teachers, TeachersReviewing)
                        val userData = call.getUserDataFromToken()
                        val newReview =
                            call.receiveNullable<ReviewDto>() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val censor = newReview.allowed()
                        if (censor.isFailure) {
                            call.respond(HttpStatusCode.Forbidden, censor.exceptionOrNull()!!.message!!)
                            return@post
                        }
                        val serverReview = newReview.copy(
                            id = null,
                            userId = userData.id,
                            createdTimestamp = Utils.now,
                            comment = newReview.comment
                        )
                        val review = Repository.Reviews.addReview(serverReview)
                        if (review != null) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotModified)
                        }
                    } catch (e: Exception) {
                        logger.error(e)
                            call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                        }
                    }
                    get("/get") {
                        verify(Teachers)
                        val params = Paging.from(call)
                        val reviews = Repository.Reviews.getReviews(params).onEach { it.user!!.clearPersonalData() }
                        call.respond(reviews)
                    }
                    get("/getById") {
                        verify(Teachers)
                        val id = call.request.queryParameters["id"]?.toIntOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, "No id specified")
                        }
                        val review = Repository.Reviews.getReview(id!!).apply { this!!.user!!.clearPersonalData() }
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
                        if (teacherId == null) {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                        val reviews =
                            Repository.Reviews.getReviewsByTeacher(teacherId!!, params)
                                .onEach { it.user!!.clearPersonalData() }
                        call.respond(reviews)
                    }
                    get("/getByUser") {
                        verify(Teachers)
                        val params = Paging.from(call)
                        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                        if (userId != null) {
                            val reviews =
                                Repository.Reviews.getReviewsByUser(userId, params)
                                    .onEach { it.user!!.clearPersonalData() }
                            call.respond(reviews)
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                }
            }
        }
        route("/admin") {
            route("/users") {
                get("/get") {
                    verify(Users)
                    val paging = Paging.from(call)
                    call.respond(Repository.Users.getUsers(paging))
                }
                get("/ban") {
                    verify(Users, UsersBlocking)
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
                    verify(Users, UsersBlocking)
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
                    verify(Users, UsersRemoving)
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
                    verify(Users, UsersRolesChanging)
                    val id = call.request.queryParameters["id"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    val role =
                        call.request.queryParameters["role"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    if (role == Roles.Superuser.name) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            "Only via direct database access. This incident will be reported."
                        )
                    }
                    if (Repository.Users.setRole(id, role = Roles.valueOf(role))) {
                        call.respond(HttpStatusCode.OK)
                        logger.debug("Set role for user $id to $role")
                    } else {
                        logger.debug("Failed to set role for user $id to $role")
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
            route("/reviews") {
                get("/delete") {
                    verify(Teachers, TeachersReviewing)
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
                    verify(SS)
                    repeat(3) {
                        logger.error("WARNING: Used danger method SHUTDOWN by user: ${call.getUserDataFromToken().id}")
                    }
                    call.respond(HttpStatusCode.OK)
                    exitProcess(1488)
                }
                get("/dropDatabase") {
                    verify(SS)
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
