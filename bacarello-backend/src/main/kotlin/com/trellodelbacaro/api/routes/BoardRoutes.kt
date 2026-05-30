package com.trellodelbacaro.api.routes

import com.trellodelbacaro.api.dto.BoardRequest
import com.trellodelbacaro.api.dto.BoardResponse
import com.trellodelbacaro.application.service.BoardService
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureBoardRoutes(boardService: BoardService) {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/boards") {
                get {
                    when (val r = boardService.findAll()) {
                        is DomainResult.Success -> call.respond(r.value.map { it.toResponse() })
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
                    when (val r = boardService.findById(id)) {
                        is DomainResult.Success -> call.respond(r.value.toResponse())
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                post {
                    val req = call.receive<BoardRequest>()
                    val board = Board(workspaceId = req.workspaceId, title = req.title, color = req.color)
                    when (val r = boardService.create(board)) {
                        is DomainResult.Success -> call.respond(HttpStatusCode.Created, r.value.toResponse())
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                put("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")
                    val req = call.receive<BoardRequest>()
                    val board = Board(id = id, workspaceId = req.workspaceId, title = req.title, color = req.color)
                    when (val r = boardService.update(board)) {
                        is DomainResult.Success -> call.respond(r.value.toResponse())
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                delete("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
                    when (val r = boardService.delete(id)) {
                        is DomainResult.Success -> call.respond(HttpStatusCode.NoContent)
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }
            }
        }
    }
}

private fun Board.toResponse() = BoardResponse(
    id = id ?: 0,
    workspaceId = workspaceId,
    title = title,
    color = color,
    createdAt = createdAt
)
