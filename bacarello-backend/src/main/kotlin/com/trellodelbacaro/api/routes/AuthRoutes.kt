package com.trellodelbacaro.api.routes

import com.trellodelbacaro.api.dto.*
import com.trellodelbacaro.application.service.AuthService
import com.trellodelbacaro.domain.common.DomainResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAuthRoutes(authService: AuthService) {
    routing {
        route("/api/v1/auth") {
            post("/register") {
                val req = call.receive<RegisterRequest>()
                when (val result = authService.register(req.email, req.displayName, req.password)) {
                    is DomainResult.Success -> {
                        val u = result.value
                        call.respond(HttpStatusCode.Created, UserResponse(u.id!!, u.email, u.displayName, u.createdAt!!))
                    }
                    is DomainResult.Failure -> call.respondFailure(result)
                }
            }

            post("/login") {
                val req = call.receive<LoginRequest>()
                when (val result = authService.login(req.email, req.password)) {
                    is DomainResult.Success -> {
                        val token = result.value
                        call.respond(LoginResponse(token.token, token.expiresAt))
                    }
                    is DomainResult.Failure -> call.respondFailure(result)
                }
            }
        }
    }
}

suspend fun ApplicationCall.respondFailure(failure: DomainResult.Failure) = when (failure) {
    is DomainResult.Failure.NotFound ->
        respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", failure.message))
    is DomainResult.Failure.ValidationError ->
        respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", failure.errors.joinToString()))
    is DomainResult.Failure.Unauthorized ->
        respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", failure.message))
    is DomainResult.Failure.Forbidden ->
        respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", failure.message))
    is DomainResult.Failure.InternalError ->
        respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR", failure.exception.message ?: "Unknown error"))
}
