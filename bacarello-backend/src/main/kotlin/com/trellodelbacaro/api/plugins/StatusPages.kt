package com.trellodelbacaro.api.plugins

import com.trellodelbacaro.api.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

/**
 * Cattura le eccezioni non gestite e i body di richiesta malformati,
 * restituendo sempre il formato errore canonico { error, message, details }.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("VALIDATION_ERROR", cause.message ?: "Malformed request body")
            )
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", cause.message ?: "Unexpected error")
            )
        }
    }
}
