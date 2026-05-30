package com.trellodelbacaro.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.hours

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)

data class JwtToken(val token: String, val expiresAt: kotlinx.datetime.Instant)

class JwtProvider(val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun generate(userId: Long, email: String): JwtToken {
        val expiresAt = Clock.System.now().plus(24.hours)
        val token = JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(expiresAt.toJavaInstant())
            .sign(algorithm)
        return JwtToken(token, expiresAt)
    }

    fun verifier() = JWT.require(algorithm)
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .build()
}
