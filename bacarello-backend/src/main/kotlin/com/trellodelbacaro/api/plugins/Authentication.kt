package com.trellodelbacaro.api.plugins

import com.trellodelbacaro.infrastructure.security.JwtProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthentication(jwtProvider: JwtProvider) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtProvider.config.realm
            verifier(jwtProvider.verifier())
            validate { credential ->
                if (credential.payload.getClaim("userId").asLong() != null) JWTPrincipal(credential.payload)
                else null
            }
        }
    }
}
