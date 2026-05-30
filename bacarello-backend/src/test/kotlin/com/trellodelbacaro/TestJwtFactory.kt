package com.trellodelbacaro

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object TestJwtFactory {
    private const val SECRET = "bacarello-super-secret-change-in-prod"
    private const val ISSUER = "bacarello"
    private const val AUDIENCE = "bacarello-users"

    fun createToken(userId: Long, email: String = "test@bacaro.it"): String =
        JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(SECRET))
}
