@file:OptIn(ExperimentalSerializationApi::class)

package apu.unsession.features.blockchain.auth

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
            post("/generate-payload") {
                call.respond(Proof.generateV2())
            }

            post("/auth") {
                val message = call.receive<TonConnectMessage>()
                if (message.isValid()) {
                    call.respond(DataResponse("token"))
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
