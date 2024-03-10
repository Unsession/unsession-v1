package lol.unsession.features.application

import freemarker.cache.ClassTemplateLoader
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
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
import lol.unsession.features.db.Repository
import lol.unsession.features.db.Repository.HolyTestObject.generateTestData
import lol.unsession.features.security.Access.*
import lol.unsession.features.security.getUserDataFromToken
import lol.unsession.features.security.verify
import lol.unsession.features.user.User
import lol.unsession.getResourceUri
import lol.unsession.models.Paging
import lol.unsession.models.ReviewDto
import lol.unsession.models.TeacherDto
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
                        //val censor = newReview.allowed()
                        //if (censor.isFailure) {
                        //    call.respond(HttpStatusCode.Forbidden, censor.exceptionOrNull()!!.message!!)
                        //    return@post
                        //}
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
    }
}
