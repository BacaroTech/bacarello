package com.trellodelbacaro.api.routes

import com.trellodelbacaro.api.dto.ListRequest
import com.trellodelbacaro.api.dto.ListResponse
import com.trellodelbacaro.application.service.ListService
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.BoardList
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureListRoutes(listService: ListService) {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/boards/{boardId}/lists") {
                get {
                    val boardId = call.parameters["boardId"]?.toLongOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid boardId")
                    when (val r = listService.findByBoard(boardId)) {
                        is DomainResult.Success -> call.respond(r.value.map { it.toResponse() })
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                post {
                    val boardId = call.parameters["boardId"]?.toLongOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid boardId")
                    val req = call.receive<ListRequest>()
                    when (val r = listService.create(boardId, req.name, req.position)) {
                        is DomainResult.Success -> call.respond(HttpStatusCode.Created, r.value.toResponse())
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }
            }
        }
    }
}

private fun BoardList.toResponse() = ListResponse(
    id = id ?: 0,
    boardId = boardId,
    name = name,
    position = position
)
