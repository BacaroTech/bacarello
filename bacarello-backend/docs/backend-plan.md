# Bacarello Backend Implementation Plan — v2

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `bacarello-backend`, un'API REST Ktor per un'applicazione Trello-like con autenticazione JWT, architettura a 4 layer (domain / application / infrastructure / api) e TDD.

**Differenze rispetto a v1:**
- Architettura rinominata: `adapters/` → `domain/ / application/ / infrastructure/ / api/`
- Domain model arricchito: `Workspace > Board > List > Card` (Column → List), aggiunto `User`, `BoardMember`, `BoardRole`
- Layer `application/service/` esplicito tra route e repository
- `DomainResult` con più subtypes: aggiunto `Unauthorized`, `Forbidden`
- Error response strutturata: `{ "error": "CODE", "message": "...", "details": {} }`
- Timestamp `Instant` (kotlinx-datetime) al posto di `Long`
- JWT auth su tutte le route tranne `/health` e `/api/v1/auth/**`
- `TestJwtFactory` per generare token nei test delle route

**Architecture:** 4-layer. Dipendenze sempre verso il dominio — `api/` e `infrastructure/` dipendono da `application/` e `domain/`, mai il contrario.

**Tech Stack:** Kotlin 2.1.10, Ktor 3.2.0 (Netty + auth-jwt), Exposed 0.61.0, kotlinx-datetime 0.6.0, BCrypt (favre), H2 (test) + PostgreSQL (prod), Kotest 5.9.1, MockK 1.13.13.

---

## File Map

```
bacarello-backend/
├── CLAUDE.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── docker-compose.yml
├── src/main/
│   ├── resources/
│   │   ├── application.yaml
│   │   └── logback.xml
│   └── kotlin/com/trellodelbacaro/
│       ├── Application.kt
│       ├── domain/
│       │   ├── common/DomainResult.kt
│       │   ├── model/Entities.kt
│       │   └── repository/Repositories.kt
│       ├── application/
│       │   └── service/
│       │       ├── AuthService.kt
│       │       ├── BoardService.kt
│       │       └── CardService.kt
│       ├── infrastructure/
│       │   ├── persistence/
│       │   │   ├── Tables.kt
│       │   │   ├── ExposedUserRepository.kt
│       │   │   ├── ExposedBoardRepository.kt
│       │   │   └── ExposedCardRepository.kt
│       │   └── security/
│       │       └── JwtProvider.kt
│       └── api/
│           ├── dto/Dtos.kt
│           ├── plugins/
│           │   ├── Serialization.kt
│           │   ├── Authentication.kt
│           │   └── Database.kt
│           └── routes/
│               ├── HealthRoutes.kt
│               ├── AuthRoutes.kt
│               ├── BoardRoutes.kt
│               └── CardRoutes.kt
└── src/test/kotlin/com/trellodelbacaro/
    ├── TestDatabaseFactory.kt
    ├── TestJwtFactory.kt
    ├── infrastructure/persistence/
    │   ├── ExposedBoardRepositoryTest.kt
    │   └── ExposedCardRepositoryTest.kt
    └── api/routes/
        ├── AuthRoutesTest.kt
        └── BoardRoutesTest.kt
```

---

## Task 1: CLAUDE.md + Project Scaffold

**Files:**
- Create: `CLAUDE.md`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `src/main/resources/application.yaml`
- Create: `src/main/resources/logback.xml`
- Create: `docker-compose.yml`

> Genera il progetto base da [start.ktor.io](https://start.ktor.io) con: Content Negotiation, Kotlinx Serialization, WebSockets, Routing, Authentication JWT. Poi sostituisci tutti i file con quelli sotto.

- [ ] **Step 1: Crea `CLAUDE.md`**

```markdown
# Project: Bacarello Backend (Ktor + Kotlin)

## Stack
- Ktor 3.2.0 (Netty, auth-jwt, content negotiation, websockets)
- Kotlin 2.1.10 + Coroutines
- Exposed 0.61.0 ORM + PostgreSQL (prod) / H2 (test)
- Kotest 5.9.1 + testApplication{} per integration test
- MockK 1.13.13 per mocking
- Docker Compose per PostgreSQL locale

## Architecture: 4-layer
- domain/         → pure Kotlin, zero framework deps
- application/    → use cases / services, orchestrano i repository
- infrastructure/ → Exposed repos, JwtProvider
- api/            → Ktor routes, DTOs, plugins

## Rules
- Nessuna logica di business nei route handler: delegano sempre all'application/service
- Tutte le operazioni DB in suspend fun
- Restituire DomainResult<T>, mai lanciare eccezioni dai service
- JWT obbligatorio su tutte le route tranne /health e /api/v1/auth/**
- Error format: { "error": "CODE", "message": "...", "details": {} }
- Timestamp: ISO 8601 UTC (kotlinx-datetime Instant)

## Domain
Workspace → Board → List → Card
BoardMember con role: ADMIN | MEMBER | VIEWER
Card ha: labels, dueDate, assignees, checklist items

## Naming
- REST: nomi plurali (/boards, /boards/{id}/lists)
- Route fun: configureBoardRoutes(boardService: BoardService)
- Service: class BoardService(private val boardRepo: BoardRepository)
- Repo: class ExposedBoardRepository(private val db: Database): BoardRepository
```

- [ ] **Step 2: Crea `settings.gradle.kts`**

```kotlin
rootProject.name = "bacarello-backend"
```

- [ ] **Step 3: Crea `gradle/libs.versions.toml`**

```toml
[versions]
exposed-version = "0.61.0"
h2-version = "2.3.232"
kotlin-version = "2.1.10"
ktor-version = "3.2.0"
logback-version = "1.4.14"
kotest-version = "5.9.1"
mockk-version = "1.13.13"
kotlinx-datetime-version = "0.6.0"
bcrypt-version = "0.10.2"

[libraries]
ktor-server-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor-version" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor-version" }
ktor-server-content-negotiation = { module = "io.ktor:ktor-server-content-negotiation", version.ref = "ktor-version" }
ktor-server-netty = { module = "io.ktor:ktor-server-netty", version.ref = "ktor-version" }
ktor-server-config-yaml = { module = "io.ktor:ktor-server-config-yaml", version.ref = "ktor-version" }
ktor-server-websockets = { module = "io.ktor:ktor-server-websockets", version.ref = "ktor-version" }
ktor-server-auth = { module = "io.ktor:ktor-server-auth", version.ref = "ktor-version" }
ktor-server-auth-jwt = { module = "io.ktor:ktor-server-auth-jwt", version.ref = "ktor-version" }
ktor-server-test-host = { module = "io.ktor:ktor-server-test-host", version.ref = "ktor-version" }

exposed-core = { module = "org.jetbrains.exposed:exposed-core", version.ref = "exposed-version" }
exposed-jdbc = { module = "org.jetbrains.exposed:exposed-jdbc", version.ref = "exposed-version" }
exposed-kotlin-datetime = { module = "org.jetbrains.exposed:exposed-kotlin-datetime", version.ref = "exposed-version" }
h2 = { module = "com.h2database:h2", version.ref = "h2-version" }
postgresql = { module = "org.postgresql:postgresql", version = "42.7.7" }

kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime-version" }
bcrypt = { module = "at.favre.lib:bcrypt", version.ref = "bcrypt-version" }
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback-version" }

kotest-runner-junit5 = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest-version" }
kotest-assertions-core = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest-version" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk-version" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin-version" }
ktor = { id = "io.ktor.plugin", version.ref = "ktor-version" }
kotlin-plugin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin-version" }
```

- [ ] **Step 4: Crea `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.trellodelbacaro"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.h2)
    implementation(libs.postgresql)

    implementation(libs.kotlinx.datetime)
    implementation(libs.bcrypt)
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 5: Crea `gradle.properties`**

```properties
ktor.io.ktor.development=true
```

- [ ] **Step 6: Crea `src/main/resources/application.yaml`**

```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - com.trellodelbacaro.ApplicationKt.module

jwt:
  secret: "bacarello-super-secret-change-in-prod"
  issuer: "bacarello"
  audience: "bacarello-users"
  realm: "Bacarello API"
```

- [ ] **Step 7: Crea `src/main/resources/logback.xml`**

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="trace">
        <appender-ref ref="STDOUT"/>
    </root>
    <logger name="org.eclipse.jetty" level="ERROR"/>
    <logger name="io.netty" level="ERROR"/>
</configuration>
```

- [ ] **Step 8: Crea `docker-compose.yml`**

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: bacarello
      POSTGRES_USER: bacarello
      POSTGRES_PASSWORD: bacarello
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

- [ ] **Step 9: Verifica compilazione**

```bash
./gradlew build -x test
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 10: Commit**

```bash
git init && git add . && git commit -m "chore: project scaffold v2 + CLAUDE.md"
```

---

## Task 2: Domain Layer

**Files:**
- Create: `src/main/kotlin/com/trellodelbacaro/domain/common/DomainResult.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/domain/model/Entities.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/domain/repository/Repositories.kt`

- [ ] **Step 1: Crea `domain/common/DomainResult.kt`**

```kotlin
package com.trellodelbacaro.domain.common

sealed class DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>()

    sealed class Failure : DomainResult<Nothing>() {
        data class NotFound(val message: String) : Failure()
        data class ValidationError(val errors: List<String>) : Failure()
        data class Unauthorized(val message: String) : Failure()
        data class Forbidden(val message: String) : Failure()
        data class InternalError(val exception: Throwable) : Failure()
    }

    fun <R> map(transform: (T) -> R): DomainResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}
```

- [ ] **Step 2: Crea `domain/model/Entities.kt`**

```kotlin
package com.trellodelbacaro.domain.model

import kotlinx.datetime.Instant

data class User(
    val id: Long? = null,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val createdAt: Instant? = null
)

enum class BoardRole { ADMIN, MEMBER, VIEWER }

data class Workspace(
    val id: Long? = null,
    val name: String,
    val ownerId: Long,
    val createdAt: Instant? = null
)

data class Board(
    val id: Long? = null,
    val workspaceId: Long,
    val title: String,
    val color: String = "#0079BF",
    val createdAt: Instant? = null,
    val lists: List<BoardList> = emptyList()
)

data class BoardMember(
    val boardId: Long,
    val userId: Long,
    val role: BoardRole
)

data class BoardList(
    val id: Long? = null,
    val boardId: Long,
    val name: String,
    val position: Int,
    val cards: List<Card> = emptyList()
)

data class Card(
    val id: Long? = null,
    val listId: Long,
    val title: String,
    val description: String? = null,
    val position: Int,
    val dueDate: Instant? = null,
    val createdAt: Instant? = null,
    val checklist: List<ChecklistItem> = emptyList()
)

data class ChecklistItem(
    val id: Long? = null,
    val cardId: Long,
    val label: String,
    val isChecked: Boolean = false,
    val position: Int
)
```

- [ ] **Step 3: Crea `domain/repository/Repositories.kt`**

```kotlin
package com.trellodelbacaro.domain.repository

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.*

interface UserRepository {
    suspend fun findByEmail(email: String): DomainResult<User>
    suspend fun create(user: User): DomainResult<User>
}

interface BoardRepository {
    suspend fun findAll(): DomainResult<List<Board>>
    suspend fun findById(id: Long): DomainResult<Board>
    suspend fun create(board: Board): DomainResult<Board>
    suspend fun update(board: Board): DomainResult<Board>
    suspend fun delete(id: Long): DomainResult<Unit>
}

interface CardRepository {
    suspend fun findById(id: Long): DomainResult<Card>
    suspend fun create(listId: Long, card: Card): DomainResult<Card>
    suspend fun update(card: Card): DomainResult<Card>
    suspend fun delete(id: Long): DomainResult<Unit>
    suspend fun move(cardId: Long, targetListId: Long, newPosition: Int): DomainResult<Card>
}
```

- [ ] **Step 4: Verifica compilazione**

```bash
./gradlew compileKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/trellodelbacaro/domain/
git commit -m "feat: domain layer - entities, DomainResult, repository interfaces"
```

---

## Task 3: Infrastructure Tables + Test Helpers

**Files:**
- Create: `src/main/kotlin/com/trellodelbacaro/infrastructure/persistence/Tables.kt`
- Create: `src/test/kotlin/com/trellodelbacaro/TestDatabaseFactory.kt`
- Create: `src/test/kotlin/com/trellodelbacaro/TestJwtFactory.kt`

- [ ] **Step 1: Crea `infrastructure/persistence/Tables.kt`**

```kotlin
package com.trellodelbacaro.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val displayName = varchar("display_name", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Workspaces : Table("workspaces") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val ownerId = long("owner_id").references(Users.id)
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Boards : Table("boards") {
    val id = long("id").autoIncrement()
    val workspaceId = long("workspace_id").references(Workspaces.id)
    val title = varchar("title", 255)
    val color = varchar("color", 7).default("#0079BF")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object BoardMembers : Table("board_members") {
    val boardId = long("board_id").references(Boards.id)
    val userId = long("user_id").references(Users.id)
    val role = varchar("role", 20).default("MEMBER")
    override val primaryKey = PrimaryKey(boardId, userId)
}

object Lists : Table("lists") {
    val id = long("id").autoIncrement()
    val boardId = long("board_id").references(Boards.id)
    val name = varchar("name", 255)
    val position = integer("position")
    override val primaryKey = PrimaryKey(id)
}

object Cards : Table("cards") {
    val id = long("id").autoIncrement()
    val listId = long("list_id").references(Lists.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val position = integer("position")
    val dueDate = timestamp("due_date").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ChecklistItems : Table("checklist_items") {
    val id = long("id").autoIncrement()
    val cardId = long("card_id").references(Cards.id)
    val label = varchar("label", 255)
    val isChecked = bool("is_checked").default(false)
    val position = integer("position")
    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 2: Crea `TestDatabaseFactory.kt`**

```kotlin
package com.trellodelbacaro

import com.trellodelbacaro.infrastructure.persistence.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object TestDatabaseFactory {
    fun create(): Database {
        val db = Database.connect(
            url = "jdbc:h2:mem:test_${System.nanoTime()};DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "root",
            password = ""
        )
        transaction(db) {
            SchemaUtils.create(Users, Workspaces, Boards, BoardMembers, Lists, Cards, ChecklistItems)
        }
        return db
    }
}
```

- [ ] **Step 3: Crea `TestJwtFactory.kt`**

```kotlin
package com.trellodelbacaro

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object TestJwtFactory {
    private const val SECRET = "bacarello-super-secret-change-in-prod"
    private const val ISSUER = "bacarello"
    private const val AUDIENCE = "bacarello-users"

    fun createToken(userId: Long, email: String = "test@bacaro.it"): String =
        JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(SECRET))
}
```

- [ ] **Step 4: Verifica compilazione**

```bash
./gradlew compileTestKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/trellodelbacaro/infrastructure/persistence/Tables.kt
git add src/test/kotlin/com/trellodelbacaro/
git commit -m "feat: persistence tables schema + TestDatabaseFactory + TestJwtFactory"
```

---

## Task 4: ExposedBoardRepository (TDD)

**Files:**
- Create: `src/test/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedBoardRepositoryTest.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedBoardRepository.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedUserRepository.kt`

> Prima creiamo `ExposedUserRepository` (senza TDD separato) perché serve come dipendenza per creare i dati di test (un utente → un workspace → una board).

- [ ] **Step 1: Crea `ExposedUserRepository.kt`**

```kotlin
package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.User
import com.trellodelbacaro.domain.repository.UserRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedUserRepository(private val database: Database) : UserRepository {

    override suspend fun findByEmail(email: String): DomainResult<User> = runCatching {
        transaction(database) {
            Users.selectAll().where { Users.email eq email }.singleOrNull()
                ?.let { DomainResult.Success(it.toUser()) }
                ?: DomainResult.Failure.NotFound("User with email $email not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(user: User): DomainResult<User> = runCatching {
        transaction(database) {
            val now = Clock.System.now()
            val newId = Users.insert {
                it[email] = user.email
                it[displayName] = user.displayName
                it[passwordHash] = user.passwordHash
                it[createdAt] = now
            } get Users.id
            DomainResult.Success(user.copy(id = newId, createdAt = now))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        email = this[Users.email],
        displayName = this[Users.displayName],
        passwordHash = this[Users.passwordHash],
        createdAt = this[Users.createdAt]
    )
}
```

- [ ] **Step 2: Scrivi il test che fallisce**

```kotlin
// src/test/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedBoardRepositoryTest.kt
package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import com.trellodelbacaro.domain.model.User
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock

class ExposedBoardRepositoryTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val repo = ExposedBoardRepository(db)
    var workspaceId: Long = -1L

    beforeSpec {
        // Crea user + workspace di supporto
        transaction(db) {
            val now = Clock.System.now()
            val userId = Users.insert {
                it[email] = "test@bacaro.it"
                it[displayName] = "Tester"
                it[passwordHash] = "hash"
                it[createdAt] = now
            } get Users.id
            workspaceId = Workspaces.insert {
                it[name] = "Test Workspace"
                it[ownerId] = userId
                it[createdAt] = now
            } get Workspaces.id
        }
    }

    "create should return board with generated id" {
        val result = repo.create(Board(workspaceId = workspaceId, title = "Il Bacaro", color = "#0079BF"))
        result.shouldBeInstanceOf<DomainResult.Success<Board>>()
        result.value.title shouldBe "Il Bacaro"
        result.value.id shouldBe 1L
    }

    "findById should return the created board" {
        val created = (repo.create(Board(workspaceId = workspaceId, title = "Spritz", color = "#FF6347")) as DomainResult.Success).value
        val result = repo.findById(created.id!!)
        result.shouldBeInstanceOf<DomainResult.Success<Board>>()
        result.value.title shouldBe "Spritz"
    }

    "findById should return NotFound for unknown id" {
        val result = repo.findById(9999L)
        result.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "findAll should return all created boards" {
        val result = repo.findAll()
        result.shouldBeInstanceOf<DomainResult.Success<List<Board>>>()
        (result.value.size >= 1) shouldBe true
    }

    "update should modify the board title" {
        val created = (repo.create(Board(workspaceId = workspaceId, title = "Old", color = "#000")) as DomainResult.Success).value
        val updated = repo.update(created.copy(title = "New"))
        updated.shouldBeInstanceOf<DomainResult.Success<Board>>()
        updated.value.title shouldBe "New"
    }

    "update should return NotFound for unknown id" {
        val result = repo.update(Board(id = 9999L, workspaceId = workspaceId, title = "X", color = "#000"))
        result.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "delete should remove the board" {
        val created = (repo.create(Board(workspaceId = workspaceId, title = "ToDelete", color = "#ccc")) as DomainResult.Success).value
        repo.delete(created.id!!).shouldBeInstanceOf<DomainResult.Success<Unit>>()
        repo.findById(created.id!!).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "delete should return NotFound for unknown id" {
        repo.delete(9999L).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }
})
```

- [ ] **Step 3: Esegui il test per verificare che fallisca**

```bash
./gradlew test --tests "com.trellodelbacaro.infrastructure.persistence.ExposedBoardRepositoryTest"
```
Expected: FAIL — `Unresolved reference: ExposedBoardRepository`

- [ ] **Step 4: Implementa `ExposedBoardRepository.kt`**

```kotlin
package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import com.trellodelbacaro.domain.repository.BoardRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedBoardRepository(private val database: Database) : BoardRepository {

    override suspend fun findAll(): DomainResult<List<Board>> = runCatching {
        transaction(database) {
            DomainResult.Success(Boards.selectAll().map { it.toBoard() })
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun findById(id: Long): DomainResult<Board> = runCatching {
        transaction(database) {
            Boards.selectAll().where { Boards.id eq id }.singleOrNull()
                ?.let { DomainResult.Success(it.toBoard()) }
                ?: DomainResult.Failure.NotFound("Board $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(board: Board): DomainResult<Board> = runCatching {
        transaction(database) {
            val now = Clock.System.now()
            val newId = Boards.insert {
                it[workspaceId] = board.workspaceId
                it[title] = board.title
                it[color] = board.color
                it[createdAt] = now
            } get Boards.id
            DomainResult.Success(board.copy(id = newId, createdAt = now))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun update(board: Board): DomainResult<Board> = runCatching {
        transaction(database) {
            val id = board.id ?: return@transaction DomainResult.Failure.ValidationError(listOf("id required"))
            val rows = Boards.update({ Boards.id eq id }) {
                it[title] = board.title
                it[color] = board.color
            }
            if (rows > 0) DomainResult.Success(board) else DomainResult.Failure.NotFound("Board $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun delete(id: Long): DomainResult<Unit> = runCatching {
        transaction(database) {
            val rows = Boards.deleteWhere { Boards.id eq id }
            if (rows > 0) DomainResult.Success(Unit) else DomainResult.Failure.NotFound("Board $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toBoard() = Board(
        id = this[Boards.id],
        workspaceId = this[Boards.workspaceId],
        title = this[Boards.title],
        color = this[Boards.color],
        createdAt = this[Boards.createdAt]
    )
}
```

- [ ] **Step 5: Esegui i test per verificare che passino**

```bash
./gradlew test --tests "com.trellodelbacaro.infrastructure.persistence.ExposedBoardRepositoryTest"
```
Expected: `8 tests passed`

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/trellodelbacaro/infrastructure/persistence/
git add src/test/kotlin/com/trellodelbacaro/infrastructure/
git commit -m "feat: ExposedBoardRepository + ExposedUserRepository - TDD"
```

---

## Task 5: Application Services + Infrastructure Security

**Files:**
- Create: `src/main/kotlin/com/trellodelbacaro/infrastructure/security/JwtProvider.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/application/service/AuthService.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/application/service/BoardService.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/application/service/CardService.kt`

- [ ] **Step 1: Crea `infrastructure/security/JwtProvider.kt`**

```kotlin
package com.trellodelbacaro.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.hours

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)

data class JwtToken(val token: String, val expiresAt: kotlinx.datetime.Instant)

class JwtProvider(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun generate(userId: Long, email: String): JwtToken {
        val expiresAt = Clock.System.now().plus(24.hours)
        val token = JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(expiresAt.toJavaInstant())
            .sign(algorithm)
        return JwtToken(token, expiresAt)
    }

    fun verifier() = JWT.require(algorithm)
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .build()
}
```

- [ ] **Step 2: Crea `application/service/AuthService.kt`**

```kotlin
package com.trellodelbacaro.application.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.User
import com.trellodelbacaro.domain.repository.UserRepository
import com.trellodelbacaro.infrastructure.security.JwtProvider
import com.trellodelbacaro.infrastructure.security.JwtToken

class AuthService(
    private val userRepo: UserRepository,
    private val jwtProvider: JwtProvider
) {
    suspend fun register(email: String, displayName: String, password: String): DomainResult<User> {
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        return userRepo.create(User(email = email, displayName = displayName, passwordHash = hash))
    }

    suspend fun login(email: String, password: String): DomainResult<JwtToken> {
        return when (val result = userRepo.findByEmail(email)) {
            is DomainResult.Success -> {
                val user = result.value
                val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
                if (verified) DomainResult.Success(jwtProvider.generate(user.id!!, user.email))
                else DomainResult.Failure.Unauthorized("Invalid credentials")
            }
            is DomainResult.Failure -> result
        }
    }
}
```

- [ ] **Step 3: Crea `application/service/BoardService.kt`**

```kotlin
package com.trellodelbacaro.application.service

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import com.trellodelbacaro.domain.repository.BoardRepository

class BoardService(private val boardRepo: BoardRepository) {
    suspend fun findAll(): DomainResult<List<Board>> = boardRepo.findAll()
    suspend fun findById(id: Long): DomainResult<Board> = boardRepo.findById(id)
    suspend fun create(board: Board): DomainResult<Board> = boardRepo.create(board)
    suspend fun update(board: Board): DomainResult<Board> = boardRepo.update(board)
    suspend fun delete(id: Long): DomainResult<Unit> = boardRepo.delete(id)
}
```

- [ ] **Step 4: Crea `application/service/CardService.kt`**

```kotlin
package com.trellodelbacaro.application.service

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Card
import com.trellodelbacaro.domain.repository.CardRepository

class CardService(private val cardRepo: CardRepository) {
    suspend fun findById(id: Long): DomainResult<Card> = cardRepo.findById(id)
    suspend fun create(listId: Long, card: Card): DomainResult<Card> = cardRepo.create(listId, card)
    suspend fun update(card: Card): DomainResult<Card> = cardRepo.update(card)
    suspend fun delete(id: Long): DomainResult<Unit> = cardRepo.delete(id)
    suspend fun move(cardId: Long, targetListId: Long, newPosition: Int): DomainResult<Card> =
        cardRepo.move(cardId, targetListId, newPosition)
}
```

- [ ] **Step 5: Verifica compilazione**

```bash
./gradlew compileKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/trellodelbacaro/application/
git add src/main/kotlin/com/trellodelbacaro/infrastructure/security/
git commit -m "feat: application services (Auth, Board, Card) + JwtProvider"
```

---

## Task 6: API Plugins + Application.kt + Routes Auth/Health

**Files:**
- Create: `src/main/kotlin/com/trellodelbacaro/api/plugins/Serialization.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/api/plugins/Authentication.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/api/plugins/Database.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/api/dto/Dtos.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/api/routes/HealthRoutes.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/api/routes/AuthRoutes.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/Application.kt`
- Create: `src/test/kotlin/com/trellodelbacaro/api/routes/AuthRoutesTest.kt`

- [ ] **Step 1: Crea `api/plugins/Serialization.kt`**

```kotlin
package com.trellodelbacaro.api.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
    }
}
```

- [ ] **Step 2: Crea `api/plugins/Authentication.kt`**

```kotlin
package com.trellodelbacaro.api.plugins

import com.trellodelbacaro.infrastructure.security.JwtProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthentication(jwtProvider: JwtProvider) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtProvider.config.realm
            verifier(jwtProvider.verifier())
            validate { credential ->
                if (credential.payload.getClaim("userId").asLong() != null) JWTPrincipal(credential.payload)
                else null
            }
        }
    }
}
```

- [ ] **Step 3: Crea `api/plugins/Database.kt`**

```kotlin
package com.trellodelbacaro.api.plugins

import com.trellodelbacaro.infrastructure.persistence.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase(): Database {
    val database = Database.connect(
        url = "jdbc:h2:mem:bacarello;DB_CLOSE_DELAY=-1;",
        driver = "org.h2.Driver",
        user = "root",
        password = ""
    )
    transaction(database) {
        SchemaUtils.create(Users, Workspaces, Boards, BoardMembers, Lists, Cards, ChecklistItems)
    }
    return database
}
```

- [ ] **Step 4: Crea `api/dto/Dtos.kt`**

```kotlin
package com.trellodelbacaro.api.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// Error
@Serializable
data class ErrorResponse(val error: String, val message: String, val details: Map<String, String> = emptyMap())

// Auth
@Serializable
data class RegisterRequest(val email: String, val displayName: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val expiresAt: Instant)

@Serializable
data class UserResponse(val id: Long, val email: String, val displayName: String, val createdAt: Instant)

// Board
@Serializable
data class BoardRequest(val workspaceId: Long, val title: String, val color: String = "#0079BF")

@Serializable
data class BoardResponse(val id: Long, val workspaceId: Long, val title: String, val color: String, val createdAt: Instant?)

// Card
@Serializable
data class CardRequest(val title: String, val description: String? = null, val position: Int, val dueDate: Instant? = null)

@Serializable
data class CardResponse(val id: Long, val listId: Long, val title: String, val description: String?, val position: Int, val dueDate: Instant?, val createdAt: Instant?)

@Serializable
data class MoveCardRequest(val listId: Long, val position: Int)
```

- [ ] **Step 5: Crea `api/routes/HealthRoutes.kt`**

```kotlin
package com.trellodelbacaro.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String)

fun Application.configureHealthRoutes() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse("ok"))
        }
    }
}
```

- [ ] **Step 6: Scrivi il test per AuthRoutes che fallisce**

```kotlin
// src/test/kotlin/com/trellodelbacaro/api/routes/AuthRoutesTest.kt
package com.trellodelbacaro.api.routes

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.api.dto.LoginResponse
import com.trellodelbacaro.api.dto.UserResponse
import com.trellodelbacaro.api.plugins.configureSerialization
import com.trellodelbacaro.application.service.AuthService
import com.trellodelbacaro.infrastructure.persistence.ExposedUserRepository
import com.trellodelbacaro.infrastructure.security.JwtConfig
import com.trellodelbacaro.infrastructure.security.JwtProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json

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
```

- [ ] **Step 7: Esegui il test per verificare che fallisca**

```bash
./gradlew test --tests "com.trellodelbacaro.api.routes.AuthRoutesTest"
```
Expected: FAIL — `Unresolved reference: configureAuthRoutes`

- [ ] **Step 8: Crea `api/routes/AuthRoutes.kt`**

```kotlin
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
```

- [ ] **Step 9: Crea `Application.kt` stub**

```kotlin
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
        secret = environment.config.property("jwt.secret").getString(),
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        realm = environment.config.property("jwt.realm").getString()
    )
    val jwtProvider = JwtProvider(jwtConfig)
    val database = configureDatabase()

    val userRepo = ExposedUserRepository(database)
    val boardRepo = ExposedBoardRepository(database)
    val cardRepo = ExposedCardRepository(database)

    val authService = AuthService(userRepo, jwtProvider)
    val boardService = BoardService(boardRepo)
    val cardService = CardService(cardRepo)

    configureSerialization()
    configureAuthentication(jwtProvider)
    configureHealthRoutes()
    configureAuthRoutes(authService)
    configureBoardRoutes(boardService)    // aggiunto Task 7
    configureCardRoutes(cardService)      // aggiunto Task 8
}
```

- [ ] **Step 10: Esegui i test auth**

```bash
./gradlew test --tests "com.trellodelbacaro.api.routes.AuthRoutesTest"
```
Expected: `4 tests passed`

- [ ] **Step 11: Commit**

```bash
git add src/main/kotlin/com/trellodelbacaro/
git add src/test/kotlin/com/trellodelbacaro/api/routes/AuthRoutesTest.kt
git commit -m "feat: API plugins + Auth routes + Health route - TDD"
```

---

## Task 7: Board Routes con JWT (TDD)

**Files:**
- Create: `src/main/kotlin/com/trellodelbacaro/api/routes/BoardRoutes.kt`
- Create: `src/test/kotlin/com/trellodelbacaro/api/routes/BoardRoutesTest.kt`

- [ ] **Step 1: Scrivi i test che falliscono**

```kotlin
// src/test/kotlin/com/trellodelbacaro/api/routes/BoardRoutesTest.kt
package com.trellodelbacaro.api.routes

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.TestJwtFactory
import com.trellodelbacaro.api.plugins.configureAuthentication
import com.trellodelbacaro.api.plugins.configureSerialization
import com.trellodelbacaro.application.service.BoardService
import com.trellodelbacaro.infrastructure.persistence.ExposedBoardRepository
import com.trellodelbacaro.infrastructure.security.JwtConfig
import com.trellodelbacaro.infrastructure.security.JwtProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*

class BoardRoutesTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val boardService = BoardService(ExposedBoardRepository(db))
    val jwtConfig = JwtConfig("bacarello-super-secret-change-in-prod", "bacarello", "bacarello-users", "Bacarello API")
    val jwtProvider = JwtProvider(jwtConfig)
    val token = TestJwtFactory.createToken(userId = 1L)

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
            // Prepara workspace (id=1 assunto già creato via TestDatabaseFactory seed o inline)
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
            client.post("/api/v1/boards") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"workspaceId":1,"title":"ToDelete","color":"#ccc"}""")
            }
            val response = client.delete("/api/v1/boards/1") { bearerAuth(token) }
            response.status shouldBe HttpStatusCode.NoContent
        }
    }
})
```

- [ ] **Step 2: Esegui il test per verificare che fallisca**

```bash
./gradlew test --tests "com.trellodelbacaro.api.routes.BoardRoutesTest"
```
Expected: FAIL — `Unresolved reference: configureBoardRoutes`

- [ ] **Step 3: Implementa `api/routes/BoardRoutes.kt`**

```kotlin
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
import kotlinx.datetime.Instant

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

private fun Board.toResponse() = BoardResponse(id = id ?: 0, workspaceId = workspaceId, title = title, color = color, createdAt = createdAt)
```

- [ ] **Step 4: Esegui i test per verificare che passino**

```bash
./gradlew test --tests "com.trellodelbacaro.api.routes.BoardRoutesTest"
```
Expected: `6 tests passed`

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/trellodelbacaro/api/routes/BoardRoutes.kt
git add src/test/kotlin/com/trellodelbacaro/api/routes/BoardRoutesTest.kt
git commit -m "feat: board REST routes con JWT auth - TDD"
```

---

## Task 8: ExposedCardRepository + Card Routes (TDD)

**Files:**
- Create: `src/test/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedCardRepositoryTest.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedCardRepository.kt`
- Create: `src/main/kotlin/com/trellodelbacaro/api/routes/CardRoutes.kt`

- [ ] **Step 1: Scrivi il test repository che fallisce**

```kotlin
// src/test/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedCardRepositoryTest.kt
package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Card
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedCardRepositoryTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val repo = ExposedCardRepository(db)
    var listId: Long = -1L

    beforeSpec {
        transaction(db) {
            val now = Clock.System.now()
            val userId = Users.insert {
                it[email] = "card@bacaro.it"; it[displayName] = "T"; it[passwordHash] = "h"; it[createdAt] = now
            } get Users.id
            val wsId = Workspaces.insert {
                it[name] = "WS"; it[ownerId] = userId; it[createdAt] = now
            } get Workspaces.id
            val boardId = Boards.insert {
                it[workspaceId] = wsId; it[title] = "B"; it[color] = "#000"; it[createdAt] = now
            } get Boards.id
            listId = Lists.insert {
                it[Lists.boardId] = boardId; it[name] = "To Do"; it[position] = 0
            } get Lists.id
        }
    }

    "create should return card with generated id" {
        val result = repo.create(listId, Card(listId = listId, title = "Spritz card", position = 0))
        result.shouldBeInstanceOf<DomainResult.Success<Card>>()
        result.value.title shouldBe "Spritz card"
    }

    "findById should return the created card" {
        val created = (repo.create(listId, Card(listId = listId, title = "Baccalà", position = 1)) as DomainResult.Success).value
        repo.findById(created.id!!).shouldBeInstanceOf<DomainResult.Success<Card>>()
    }

    "findById should return NotFound for unknown id" {
        repo.findById(9999L).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "update should modify the card title" {
        val created = (repo.create(listId, Card(listId = listId, title = "Old", position = 2)) as DomainResult.Success).value
        val result = repo.update(created.copy(title = "New"))
        result.shouldBeInstanceOf<DomainResult.Success<Card>>()
        result.value.title shouldBe "New"
    }

    "delete should remove the card" {
        val created = (repo.create(listId, Card(listId = listId, title = "ToDelete", position = 3)) as DomainResult.Success).value
        repo.delete(created.id!!).shouldBeInstanceOf<DomainResult.Success<Unit>>()
        repo.findById(created.id!!).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "move should change listId and position" {
        val created = (repo.create(listId, Card(listId = listId, title = "Moving", position = 0)) as DomainResult.Success).value
        val list2Id = transaction(db) {
            val now = Clock.System.now()
            Lists.insert { it[boardId] = 1L; it[name] = "Done"; it[position] = 1 } get Lists.id
        }
        repo.move(created.id!!, list2Id, 0).shouldBeInstanceOf<DomainResult.Success<Card>>()
    }
})
```

- [ ] **Step 2: Esegui il test per verificare che fallisca**

```bash
./gradlew test --tests "com.trellodelbacaro.infrastructure.persistence.ExposedCardRepositoryTest"
```
Expected: FAIL — `Unresolved reference: ExposedCardRepository`

- [ ] **Step 3: Implementa `ExposedCardRepository.kt`**

```kotlin
package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Card
import com.trellodelbacaro.domain.repository.CardRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedCardRepository(private val database: Database) : CardRepository {

    override suspend fun findById(id: Long): DomainResult<Card> = runCatching {
        transaction(database) {
            Cards.selectAll().where { Cards.id eq id }.singleOrNull()
                ?.let { DomainResult.Success(it.toCard()) }
                ?: DomainResult.Failure.NotFound("Card $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(listId: Long, card: Card): DomainResult<Card> = runCatching {
        transaction(database) {
            val now = Clock.System.now()
            val newId = Cards.insert {
                it[Cards.listId] = listId
                it[title] = card.title
                it[description] = card.description
                it[position] = card.position
                it[dueDate] = card.dueDate
                it[createdAt] = now
            } get Cards.id
            DomainResult.Success(card.copy(id = newId, listId = listId, createdAt = now))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun update(card: Card): DomainResult<Card> = runCatching {
        transaction(database) {
            val id = card.id ?: return@transaction DomainResult.Failure.ValidationError(listOf("id required"))
            val rows = Cards.update({ Cards.id eq id }) {
                it[title] = card.title
                it[description] = card.description
                it[position] = card.position
                it[dueDate] = card.dueDate
            }
            if (rows > 0) DomainResult.Success(card) else DomainResult.Failure.NotFound("Card $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun delete(id: Long): DomainResult<Unit> = runCatching {
        transaction(database) {
            val rows = Cards.deleteWhere { Cards.id eq id }
            if (rows > 0) DomainResult.Success(Unit) else DomainResult.Failure.NotFound("Card $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun move(cardId: Long, targetListId: Long, newPosition: Int): DomainResult<Card> = runCatching {
        transaction(database) {
            val rows = Cards.update({ Cards.id eq cardId }) {
                it[listId] = targetListId
                it[position] = newPosition
            }
            if (rows > 0) Cards.selectAll().where { Cards.id eq cardId }.single().let { DomainResult.Success(it.toCard()) }
            else DomainResult.Failure.NotFound("Card $cardId not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toCard() = Card(
        id = this[Cards.id],
        listId = this[Cards.listId],
        title = this[Cards.title],
        description = this[Cards.description],
        position = this[Cards.position],
        dueDate = this[Cards.dueDate],
        createdAt = this[Cards.createdAt]
    )
}
```

- [ ] **Step 4: Esegui i test repository**

```bash
./gradlew test --tests "com.trellodelbacaro.infrastructure.persistence.ExposedCardRepositoryTest"
```
Expected: `6 tests passed`

- [ ] **Step 5: Crea `api/routes/CardRoutes.kt`**

```kotlin
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
            post("/api/v1/lists/{listId}/cards") {
                val listId = call.parameters["listId"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid listId")
                val req = call.receive<CardRequest>()
                val card = Card(listId = listId, title = req.title, description = req.description, position = req.position, dueDate = req.dueDate)
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
                    val card = Card(id = id, listId = 0, title = req.title, description = req.description, position = req.position, dueDate = req.dueDate)
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
    id = id ?: 0, listId = listId, title = title,
    description = description, position = position,
    dueDate = dueDate, createdAt = createdAt
)
```

- [ ] **Step 6: Esegui tutti i test**

```bash
./gradlew test
```
Expected: tutti i test passano.

- [ ] **Step 7: Avvia e verifica manualmente**

```bash
./gradlew run
```
```bash
# Register
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"m@bacaro.it","displayName":"Michele","password":"secret123"}' | jq .

# Login → copia il token
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"m@bacaro.it","password":"secret123"}' | jq .

# GET boards (con token)
curl -s http://localhost:8080/api/v1/boards \
  -H "Authorization: Bearer <TOKEN>" | jq .
```

- [ ] **Step 8: Commit finale**

```bash
git add src/main/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedCardRepository.kt
git add src/main/kotlin/com/trellodelbacaro/api/routes/CardRoutes.kt
git add src/test/kotlin/com/trellodelbacaro/infrastructure/persistence/ExposedCardRepositoryTest.kt
git commit -m "feat: ExposedCardRepository + Card routes - TDD - MVP complete"
```

---

## Stima tempi v2

| Task | Descrizione | Min |
|---|---|---|
| 1 | CLAUDE.md + Scaffold | 5 |
| 2 | Domain layer | 4 |
| 3 | Tables + TestDatabaseFactory + TestJwtFactory | 5 |
| 4 | ExposedBoardRepository TDD | 8 |
| 5 | Application Services + JwtProvider | 5 |
| 6 | Plugins + Auth/Health routes TDD | 8 |
| 7 | Board routes con JWT TDD | 7 |
| 8 | ExposedCardRepository + Card routes TDD | 8 |
| | **Totale** | **~50 min** |

> **Per 40 minuti:** salta i test di `AuthRoutesTest` e `BoardRoutesTest` (scrivi le route direttamente senza i test in-progress), aggiungili dopo. I repository test (Task 4 e 8) restano obbligatori.

---

## P2 — Fuori scope MVP (da implementare prima del frontend)

- [ ] **Flyway migrations** — rimpiazza `SchemaUtils.create` in `Database.kt` con script SQL versionati in `resources/db/migration/`
- [ ] **PostgreSQL in produzione** — aggiorna `Database.kt` per leggere `DATABASE_URL` da env var, usa docker-compose.yml
- [ ] **Workspace routes** — `GET/POST /api/v1/workspaces`
- [ ] **List routes** — `POST /api/v1/boards/{boardId}/lists`, `PUT/DELETE /api/v1/lists/{id}`
- [ ] **Board members** — `POST/DELETE /api/v1/boards/{id}/members`
- [ ] **Checklist routes** — `POST/PUT/DELETE /api/v1/cards/{cardId}/checklist`
- [ ] **CORS** — necessario per Vue frontend su porta diversa (`ktor-server-cors`)
- [ ] **Audit finale** — missing error handling, N+1 query, input validation, missing auth
