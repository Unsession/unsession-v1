package lol.unsession.db.models

data class Config (
    val port: Int,
    val ip: String,
    val secret: String,
    val tokenLifetime: Int,
    val hashAlgorithm: String,
    val hashIterations: Int,
    val hashKeyLength: Int,
)
