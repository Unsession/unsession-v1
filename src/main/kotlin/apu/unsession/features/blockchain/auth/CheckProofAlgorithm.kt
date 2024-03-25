package apu.unsession.features.blockchain.auth

import apu.unsession.features.http.clientTonApi
import apu.unsession.utils.getLogger
import diglol.crypto.Ed25519
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*

object CheckProofAlgorithm {
    const val tonProofPrefix = "ton-proof-item-v2/"
    const val tonConnectPrefix = "ton-connect"
    val logger = getLogger("CheckProofAlgorithm")

    private fun encodeFullMessage(messageBytes: ByteArray): ByteArray {
        val messageHash = MessageDigest.getInstance("SHA-256").digest(messageBytes)
        val fullMessage = byteArrayOf(0xff.toByte(), 0xff.toByte()) + tonConnectPrefix.toByteArray() + messageHash
        return MessageDigest.getInstance("SHA-256").digest(fullMessage)
    }

    private fun constructFullMessage(message: TonConnectMessage): ByteArray {
        val workchainNumber = message.address.substringBefore(":").toInt()
        val addressFromHex = hex(message.address.substringAfter(":"))
        val workchainBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(workchainNumber).array()
        val timestampBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(message.timestamp).array()
        val domainLengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(message.domain.lengthBytes).array()

        val messageBytes = tonProofPrefix.toByteArray() +
                workchainBytes +
                addressFromHex +
                domainLengthBytes +
                message.domain.value.toByteArray() +
                timestampBytes +
                message.payload.toByteArray()

        return encodeFullMessage(messageBytes)
    }

    private suspend fun verifyPayload(message: TonConnectMessage, pubKey: ByteArray): Boolean {
        return Ed25519.verify(Base64.getDecoder().decode(message.signature), pubKey, constructFullMessage(message))
    }

    private suspend fun getPublicKey(address: String): String? {
        try {
            return clientTonApi.get {
                url("v2/accounts/${address}/publickey")
            }.body<PublicKeyResponse>().public_key
        } catch (e: Exception) {
            logger.error("Failed to get public key for address $address", e)
        }
        return null
    }

    private fun decodePublicKey(publicKey: String) = publicKey.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

    suspend fun TonConnectMessage.isValid(): Boolean {
        val pubKey = getPublicKey(address) ?: return false
        val decoded = decodePublicKey(pubKey)
        return verifyPayload(this, decoded)
    }

}
