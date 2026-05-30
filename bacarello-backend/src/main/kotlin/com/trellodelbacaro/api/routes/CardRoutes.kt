package com.trellodelbacaro.api.routes

import com.trellodelbacaro.api.dto.*
import com.trellodelbacaro.application.service.CardService
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Card
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCardRoutes(cardService: CardService) {
    routing {
        authenticate("auth-jwt") {
            get("/api/v1/lists/{listId}/cards") {
                val listId = call.parameters["listId"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid listId")
                when (val r = cardService.findByList(listId)) {
                    is DomainResult.Success -> call.respond(r.value.map { it.toResponse() })
                    is DomainResult.Failure -> call.respondFailure(r)
                }
            }

            post("/api/v1/lists/{listId}/cards") {
                val listId = call.parameters["listId"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid listId")
                val req = call.receive<CardRequest>()
                val card = Card(
                    listId = listId,
                    title = req.title,
                    description = req.description,
                    position = req.position,
                    dueDate = req.dueDate
                )
                when (val r = cardService.create(listId, card)) {
                    is DomainResult.Success -> call.respond(HttpStatusCode.Created, r.value.toResponse())
                    is DomainResult.Failure -> call.respondFailure(r)
                }
            }

            route("/api/v1/cards/{id}") {
                get {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
                    when (val r = cardService.findById(id)) {
                        is DomainResult.Success -> call.respond(r.value.toResponse())
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                put {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")
                    val req = call.receive<CardRequest>()
                    val card = Card(
                        id = id,
                        listId = 0,
                        title = req.title,
                        description = req.description,
                        position = req.position,
                        dueDate = req.dueDate
                    )
                    when (val r = cardService.update(card)) {
                        is DomainResult.Success -> call.respond(r.value.toResponse())
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }

                delete {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
                    when (val r = cardService.delete(id)) {
                        is DomainResult.Success -> call.respond(HttpStatusCode.NoContent)
                        is DomainResult.Failure -> call.respondFailure(r)
                    }
                }
            }

            put("/api/v1/cards/{id}/move") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")
                val req = call.receive<MoveCardRequest>()
                when (val r = cardService.move(id, req.listId, req.position)) {
                    is DomainResult.Success -> call.respond(r.value.toResponse())
                    is DomainResult.Failure -> call.respondFailure(r)
                }
            }
        }
    }
}

private fun Card.toResponse() = CardResponse(
    id = id ?: 0,
    listId = listId,
    title = title,
    description = description,
    position = position,
    dueDate = dueDate,
    createdAt = createdAt
)
