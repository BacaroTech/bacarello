package com.trellodelbacaro.api.routes

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.TestJwtFactory
import com.trellodelbacaro.api.plugins.configureAuthentication
import com.trellodelbacaro.api.plugins.configureSerialization
import com.trellodelbacaro.application.service.BoardService
import com.trellodelbacaro.infrastructure.persistence.ExposedBoardRepository
import com.trellodelbacaro.infrastructure.persistence.Users
import com.trellodelbacaro.infrastructure.persistence.Workspaces
import com.trellodelbacaro.infrastructure.security.JwtConfig
import com.trellodelbacaro.infrastructure.security.JwtProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class BoardRoutesTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val boardService = BoardService(ExposedBoardRepository(db))
    val jwtConfig = JwtConfig("bacarello-super-secret-change-in-prod", "bacarello", "bacarello-users", "Bacarello API")
    val jwtProvider = JwtProvider(jwtConfig)
    val token = TestJwtFactory.createToken(userId = 1L)

    beforeSpec {
        // FK chain: serve un workspace id=1 perché le board lo referenziano
        transaction(db) {
            val now = Clock.System.now()
            val userId = Users.insert {
                it[email] = "owner@bacaro.it"; it[displayName] = "Owner"; it[passwordHash] = "h"; it[createdAt] = now
            } get Users.id
            Workspaces.insert {
                it[name] = "WS"; it[ownerId] = userId; it[createdAt] = now
            }
        }
    }

    fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureSerialization()
            configureAuthentication(jwtProvider)
            configureBoardRoutes(boardService)
        }
        block()
    }

    "GET /api/v1/boards returns 401 without token" {
        testApp {
            client.get("/api/v1/boards").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    "GET /api/v1/boards returns 200 with valid token" {
        testApp {
            val response = client.get("/api/v1/boards") { bearerAuth(token) }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "POST /api/v1/boards creates board and returns 201" {
        testApp {
            val response = client.post("/api/v1/boards") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"workspaceId":1,"title":"Il Bacaro","color":"#0079BF"}""")
            }
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText() shouldContain "Il Bacaro"
        }
    }

    "GET /api/v1/boards/{id} returns 404 for unknown board" {
        testApp {
            val response = client.get("/api/v1/boards/9999") { bearerAuth(token) }
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    "PUT /api/v1/boards/{id} updates board title" {
        testApp {
            client.post("/api/v1/boards") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"workspaceId":1,"title":"Old","color":"#000"}""")
            }
            val response = client.put("/api/v1/boards/1") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"workspaceId":1,"title":"New","color":"#000"}""")
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "New"
        }
    }

    "DELETE /api/v1/boards/{id} returns 204" {
        testApp {
            val created = client.post("/api/v1/boards") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"workspaceId":1,"title":"ToDelete","color":"#ccc"}""")
            }
            val id = Regex("\"id\"\\s*:\\s*(\\d+)").find(created.bodyAsText())!!.groupValues[1]
            val response = client.delete("/api/v1/boards/$id") { bearerAuth(token) }
            response.status shouldBe HttpStatusCode.NoContent
        }
    }
})
