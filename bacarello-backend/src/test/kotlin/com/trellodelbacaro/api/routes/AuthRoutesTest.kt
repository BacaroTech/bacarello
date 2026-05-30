package com.trellodelbacaro.api.routes

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.api.plugins.configureSerialization
import com.trellodelbacaro.application.service.AuthService
import com.trellodelbacaro.infrastructure.persistence.ExposedUserRepository
import com.trellodelbacaro.infrastructure.security.JwtConfig
import com.trellodelbacaro.infrastructure.security.JwtProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class AuthRoutesTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val jwtConfig = JwtConfig(
        secret = "bacarello-super-secret-change-in-prod",
        issuer = "bacarello",
        audience = "bacarello-users",
        realm = "Bacarello API"
    )
    val jwtProvider = JwtProvider(jwtConfig)
    val authService = AuthService(ExposedUserRepository(db), jwtProvider)

    fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureSerialization()
            configureAuthRoutes(authService)
            configureHealthRoutes()
        }
        block()
    }

    "POST /api/v1/auth/register returns 201 with user data" {
        testApp {
            val response = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"m@bacaro.it","displayName":"Michele","password":"secret123"}""")
            }
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText() shouldContain "m@bacaro.it"
        }
    }

    "POST /api/v1/auth/login returns 200 with JWT token" {
        testApp {
            client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"login@bacaro.it","displayName":"Tester","password":"secret123"}""")
            }
            val response = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"login@bacaro.it","password":"secret123"}""")
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "token"
        }
    }

    "POST /api/v1/auth/login returns 401 with wrong password" {
        testApp {
            client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"wrong@bacaro.it","displayName":"Tester","password":"secret123"}""")
            }
            val response = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"wrong@bacaro.it","password":"wrongpassword"}""")
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    "GET /health returns 200" {
        testApp {
            val response = client.get("/health")
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
