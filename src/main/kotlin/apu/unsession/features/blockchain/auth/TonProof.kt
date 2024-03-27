package apu.unsession.features.blockchain.auth

import io.github.andreypfau.kotlinx.crypto.sha2.sha256
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Proof(val payload: String) {
    companion object {
        fun generateV2(): Proof {
            val proof = Proof(sha256("UnsessionDApp.${System.currentTimeMillis()}.".toByteArray(Charsets.UTF_8)).toString())

            return proof
        }
    }
}

@Serializable
data class DataResponse(val token: String)

@Serializable
data class PublicKeyResponse(val public_key: String)

@Serializable
data class Domain(val lengthBytes: Int, val value: String)

@Serializable
data class TonConnectMessage(
    val address: String,
    val domain: Domain,
    val network: Int,
    val payload: String,
    val signature: String,
    val state_init: String,
    val timestamp: Long
)