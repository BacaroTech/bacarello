package com.trellodelbacaro

import com.trellodelbacaro.api.plugins.*
import com.trellodelbacaro.api.routes.*
import com.trellodelbacaro.application.service.*
import com.trellodelbacaro.infrastructure.persistence.*
import com.trellodelbacaro.infrastructure.security.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val jwtConfig = JwtConfig(
        secret = System.getenv("JWT_SECRET") ?: environment.config.property("jwt.secret").getString(),
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        realm = environment.config.property("jwt.realm").getString()
    )
    val jwtProvider = JwtProvider(jwtConfig)
    val database = configureDatabase()

    val userRepo = ExposedUserRepository(database)
    val authService = AuthService(userRepo, jwtProvider)

    configureSerialization()
    configureAuthentication(jwtProvider)
    configureHealthRoutes()
    configureAuthRoutes(authService)
    // configureBoardRoutes / configureCardRoutes vengono aggiunti nei Task 7 e 8
}
