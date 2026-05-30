package com.trellodelbacaro.api.routes

import com.trellodelbacaro.api.dto.ErrorResponse
import com.trellodelbacaro.api.dto.WorkspaceRequest
import com.trellodelbacaro.api.dto.WorkspaceResponse
import com.trellodelbacaro.application.service.WorkspaceService
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Workspace
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** Estrae lo userId dal claim del JWT autenticato. */
fun ApplicationCall.authenticatedUserId(): Long? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()

fun Application.configureWorkspaceRoutes(workspaceService: WorkspaceService) {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/workspaces") {
                get {
                    val ownerId = call.authenticatedUserId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Missing userId claim"))
                    when (val r = workspaceService.findAllByOwner(ownerId)) {
                        is DomainResult.Success -> call.respond(r.value.map { it.toResponse() })
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                post {
                    val ownerId = call.authenticatedUserId()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Missing userId claim"))
                    val req = call.receive<WorkspaceRequest>()
                    when (val r = workspaceService.create(req.name, ownerId)) {
                        is DomainResult.Success -> call.respond(HttpStatusCode.Created, r.value.toResponse())
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }
            }
        }
    }
}

private fun Workspace.toResponse() = WorkspaceResponse(
    id = id ?: 0,
    name = name,
    ownerId = ownerId,
    createdAt = createdAt
)
