@file:OptIn(ExperimentalSerializationApi::class)

package apu.unsession.features.blockchain.auth

import apu.unsession.application.logger
import apu.unsession.features.blockchain.auth.CheckProofAlgorithm.isValid
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi


fun Application.configureBlockchain() {
    routing {
        route("/ton") {
            get("/generatePayload") {
                call.respond(Proof.generateV2())
            }

            post("/auth") {
                var message: TonConnectMessage? = null
                try {
                    message = call.receive<TonConnectMessage>()
                } catch (e: Exception) {
                    logger.error(e.message)
                    call.respond(HttpStatusCode.BadRequest, e.stackTrace)
                }
                if (message!!.isValid()) {
                    call.respond(DataResponse("token"))
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
