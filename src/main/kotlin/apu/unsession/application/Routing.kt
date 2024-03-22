package apu.unsession.application

import apu.unsession.Utils
import apu.unsession.features.db.Repository
import apu.unsession.features.db.Repository.HolyTestObject.generateTestData
import apu.unsession.features.security.getUserDataFromToken
import apu.unsession.features.security.roles.Access.*
import apu.unsession.features.security.verify
import apu.unsession.features.user.User
import apu.unsession.getResourceUri
import apu.unsession.models.Paging
import apu.unsession.models.ReviewDto
import apu.unsession.models.TeacherDto
import apu.unsession.utils.getLogger
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

val logger = getLogger("Routing")

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)

fun Application.configureRouting() {
    install(StatusPages) {
        status(HttpStatusCode.OK) { _ ->
            call.respondText(text = "200: It’s окей", status = HttpStatusCode.OK)
        }
        status(HttpStatusCode.Created) { _ ->
            call.respondText(text = "201: Да, мне было страшно, но я это сделал", status = HttpStatusCode.Created)
        }
        status(HttpStatusCode.Accepted) { _ ->
            call.respondText(text = "202: Супергуд", status = HttpStatusCode.Accepted)
        }
        status(HttpStatusCode.BadRequest) { _ ->
            call.respondText(text = "400: Это была, скорее всего, бюрократическая просто спешка", status = HttpStatusCode.BadRequest)
        }
        status(HttpStatusCode.Unauthorized) { _ ->
            call.respondText(text = "401: Это было не просто смело, это было пиздец как смело", status = HttpStatusCode.Unauthorized)
        }
        status(HttpStatusCode.PaymentRequired) { _ ->
            call.respondText(text = "402: Ну это я какие-то копейки получил просто от этого", status = HttpStatusCode.PaymentRequired)
        }
        status(HttpStatusCode.Forbidden) { _ ->
            call.respondText(text = "403: Ты совершил страшное преступление…", status = HttpStatusCode.Forbidden)
        }
        status(HttpStatusCode.NotFound) { _ ->
            call.respondText(text = "404: Ни-ху-я, вот просто ни-ху-я", status = HttpStatusCode.NotFound)
        }
        status(HttpStatusCode.MethodNotAllowed) { _ ->
            call.respondText(text = "405: Вот мне лично это не интересно, за других сказать не могу", status = HttpStatusCode.MethodNotAllowed)
        }
        status(HttpStatusCode.NotAcceptable) { _ ->
            call.respondText(text = "406: Не приглашайте меня ни премьер-министром, ни главой Центробанка", status = HttpStatusCode.NotAcceptable)
        }
        status(HttpStatusCode.RequestTimeout) { _ ->
            call.respondText(text = "408: Это конечно печально, это печально", status = HttpStatusCode.RequestTimeout)
        }
        status(HttpStatusCode.Conflict) { _ ->
            call.respondText(text = "409: То есть, понимаешь, ты должен страдать, и тогда... Ну понимаешь да?", status = HttpStatusCode.Conflict)
        }
        status(HttpStatusCode.Gone) { _ ->
            call.respondText(text = "410: Это была, скорее всего, бюрократическая просто спешка. Они меня всунули, не разобравшись кто я есть", status = HttpStatusCode.Gone)
        }
        status(HttpStatusCode.UnsupportedMediaType) { _ ->
            call.respondText(text = "415: Выродок ты откуда такой? Ты же наш, Сибирский. Почему такой выродок?", status = HttpStatusCode.UnsupportedMediaType)
        }
        status(HttpStatusCode.TooManyRequests) { _ ->
            call.respondText(text = "429: Ну это пиздец какой-то просто! Ну сколько можно!", status = HttpStatusCode.TooManyRequests)
        }
        status(HttpStatusCode.InternalServerError) { _ ->
            call.respondText(text = "500: Я ошибся! Я могу один раз ошибиться?", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotImplemented) { _ ->
            call.respondText(text = "501: Вот мне лично это не интересно, за других сказать не могу", status = HttpStatusCode.NotImplemented)
        }
        status(HttpStatusCode.BadGateway) { _ ->
            call.respondText(text = "502: Галицкий вообще ничего никогда не сказал", status = HttpStatusCode.BadGateway)
        }
        status(HttpStatusCode.ServiceUnavailable) { _ ->
            call.respondText(text = "503: Миш, мне похуй, я так чувствую", status = HttpStatusCode.ServiceUnavailable)
        }
        status(HttpStatusCode.GatewayTimeout) { _ ->
            call.respondText(text = "504: Галицкий вообще ничего никогда не сказал. Ваш герой. Вообще, блядь, никогда и ничего не сказал и сейчас молчит", status = HttpStatusCode.GatewayTimeout)
        }
        status(HttpStatusCode.InsufficientStorage) { _ ->
            call.respondText(text = "507: Я как бы не понимал весь масштаб этого пиздеца", status = HttpStatusCode.InsufficientStorage)
        }
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: Я ошибся! Я могу один раз ошибиться? - $cause", status = HttpStatusCode.InternalServerError)
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
