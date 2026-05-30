package com.trellodelbacaro.api.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

/**
 * CORS per il frontend Vue (porta diversa dal backend).
 * Origini consentite via env CORS_ALLOWED_HOSTS (csv host:porta), es: "localhost:5173,localhost:4173".
 * Se non impostata, in sviluppo si accetta qualsiasi origine (anyHost).
 * L'autenticazione viaggia nell'header Authorization (Bearer), non in cookie, quindi anyHost è sicuro qui.
 */
fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        val allowed = System.getenv("CORS_ALLOWED_HOSTS")
        if (allowed.isNullOrBlank()) {
            anyHost()
        } else {
            allowed.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { entry ->
                val parts = entry.split(":")
                val host = parts[0]
                val schemes = listOf("http", "https")
                if (parts.size > 1) allowHost("$host:${parts[1]}", schemes = schemes)
                else allowHost(host, schemes = schemes)
            }
        }
    }
}
