# Bacarello Backend — Specifiche Tecniche

> Documento di riferimento per l'implementazione. Aggiornato dopo review dell'`ai-suggests.md`.
> Versione: 2 | Data: 2026-04-08

---

## 1. Obiettivo

Backend REST per **Bacarello**, un'applicazione Trello-like. Gli utenti creano workspace, aggiungono board, organizzano le card in liste. Le card supportano etichette, scadenze, assegnazioni e checklist.

---

## 2. Stack tecnologico

| Layer | Tecnologia |
|---|---|
| Framework HTTP | Ktor 3.2.0 (Netty) |
| Linguaggio | Kotlin 2.1.10 |
| ORM | Exposed 0.61.0 |
| Migration | Flyway |
| DB produzione | PostgreSQL |
| DB test | H2 in-memory |
| Auth | JWT (ktor-server-auth-jwt) |
| Test framework | Kotest 5.9.1 + testApplication |
| Mocking | MockK 1.13.13 |
| Containerizzazione | Docker Compose |
| Serializzazione | Kotlinx Serialization (JSON) |

---

## 3. Architettura

Struttura a 4 layer. Il flusso di dipendenze va sempre verso il dominio:

```
api/ → application/ → domain/
infrastructure/ → domain/
infrastructure/ → application/
```

```
src/main/kotlin/com/trellodelbacaro/
├── domain/                     # puro Kotlin, zero dipendenze da framework
│   ├── model/                  # data class: entità di dominio
│   ├── repository/             # interfacce repository (ports)
│   └── common/                 # DomainResult, DomainError
├── application/                # use case / service layer
│   └── service/                # orchestrano repository, nessuna logica HTTP
├── infrastructure/             # implementazioni concrete
│   ├── persistence/            # Exposed ORM: tabelle, repository impl
│   └── security/               # JWT provider
└── api/                        # Ktor: routes, DTOs, middleware
    ├── dto/                    # request/response data class
    ├── routes/                 # configureBoardRoutes, configureCardRoutes...
    └── plugins/                # configureSerialization, configureAuth...
```

**Regole architetturali:**
- Nessuna logica di business nei route handler — delegano sempre all'`application/service`
- Tutte le operazioni DB sono in `suspend fun` eseguite su `Dispatchers.IO`
- Non si lancia mai un'eccezione dai service — si restituisce `DomainResult`
- JWT obbligatorio su tutti i route tranne `/health` e `/auth/**`

---

## 4. Modello di dominio

```
Workspace
│   id: Long
│   name: String
│   ownerId: Long
│   createdAt: Instant
│
└─── Board (*)
     │   id: Long
     │   workspaceId: Long
     │   title: String
     │   color: String          # hex color
     │   createdAt: Instant
     │
     ├─── BoardMember (*)       # utenti con accesso al board
     │        userId: Long
     │        role: BoardRole   # ADMIN | MEMBER | VIEWER
     │
     └─── List (*)
          │   id: Long
          │   boardId: Long
          │   name: String
          │   position: Int
          │
          └─── Card (*)
               │   id: Long
               │   listId: Long
               │   title: String
               │   description: String?
               │   position: Int
               │   dueDate: Instant?
               │   createdAt: Instant
               │
               ├─── CardAssignment (*)     # utenti assegnati alla card
               │        userId: Long
               │
               ├─── CardLabel (*)
               │        id: Long
               │        color: String
               │        text: String
               │
               └─── ChecklistItem (*)
                        id: Long
                        label: String
                        isChecked: Boolean
                        position: Int

User
    id: Long
    email: String               # unique
    displayName: String
    passwordHash: String
    createdAt: Instant
```

**Enum:**
```kotlin
enum class BoardRole { ADMIN, MEMBER, VIEWER }
```

---

## 5. Schema database (ERD testuale)

```sql
users
  id            BIGSERIAL PK
  email         VARCHAR(255) UNIQUE NOT NULL
  display_name  VARCHAR(255) NOT NULL
  password_hash VARCHAR(255) NOT NULL
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()

workspaces
  id         BIGSERIAL PK
  name       VARCHAR(255) NOT NULL
  owner_id   BIGINT FK → users.id
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

boards
  id           BIGSERIAL PK
  workspace_id BIGINT FK → workspaces.id
  title        VARCHAR(255) NOT NULL
  color        VARCHAR(7) NOT NULL DEFAULT '#0079BF'
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()

board_members
  board_id BIGINT FK → boards.id
  user_id  BIGINT FK → users.id
  role     VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
  PK (board_id, user_id)

lists
  id         BIGSERIAL PK
  board_id   BIGINT FK → boards.id
  name       VARCHAR(255) NOT NULL
  position   INT NOT NULL
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

cards
  id          BIGSERIAL PK
  list_id     BIGINT FK → lists.id
  title       VARCHAR(255) NOT NULL
  description TEXT
  position    INT NOT NULL
  due_date    TIMESTAMPTZ
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()

card_assignments
  card_id BIGINT FK → cards.id
  user_id BIGINT FK → users.id
  PK (card_id, user_id)

card_labels
  id      BIGSERIAL PK
  card_id BIGINT FK → cards.id
  color   VARCHAR(7) NOT NULL
  text    VARCHAR(100) NOT NULL

checklist_items
  id         BIGSERIAL PK
  card_id    BIGINT FK → cards.id
  label      VARCHAR(255) NOT NULL
  is_checked BOOLEAN NOT NULL DEFAULT FALSE
  position   INT NOT NULL
```

---

## 6. API REST — Endpoint completi

### Convenzioni

- Base path: `/api/v1`
- Autenticazione: `Authorization: Bearer <jwt>` — **obbligatoria** su tutto tranne `/health` e `/api/v1/auth/**`
- Content-Type: `application/json`
- Timestamp: ISO 8601 UTC (`2026-04-08T10:30:00Z`)

**Formato errore standard:**
```json
{
  "error": "NOT_FOUND",
  "message": "Board 42 not found",
  "details": {}
}
```

**Codici errore:**
| Codice | HTTP |
|---|---|
| `NOT_FOUND` | 404 |
| `VALIDATION_ERROR` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |
| `INTERNAL_ERROR` | 500 |

---

### Auth

| Method | Path | Auth | Descrizione |
|---|---|---|---|
| POST | `/api/v1/auth/register` | No | Crea account |
| POST | `/api/v1/auth/login` | No | Ritorna JWT |

**POST /auth/register** — request:
```json
{ "email": "m@bacaro.it", "displayName": "Michele", "password": "secret123" }
```
Response 201:
```json
{ "id": 1, "email": "m@bacaro.it", "displayName": "Michele", "createdAt": "2026-04-08T10:00:00Z" }
```

**POST /auth/login** — request:
```json
{ "email": "m@bacaro.it", "password": "secret123" }
```
Response 200:
```json
{ "token": "eyJ...", "expiresAt": "2026-04-09T10:00:00Z" }
```

---

### Health

| Method | Path | Auth | Descrizione |
|---|---|---|---|
| GET | `/health` | No | Liveness check |

Response 200: `{ "status": "ok" }`

---

### Workspaces

| Method | Path | Auth | Descrizione |
|---|---|---|---|
| GET | `/api/v1/workspaces` | JWT | Lista workspace dell'utente |
| POST | `/api/v1/workspaces` | JWT | Crea workspace |
| GET | `/api/v1/workspaces/{id}` | JWT | Dettaglio workspace |
| GET | `/api/v1/workspaces/{id}/boards` | JWT | Board del workspace |

**POST /workspaces** — request:
```json
{ "name": "Il Bacaro Team" }
```
Response 201:
```json
{ "id": 1, "name": "Il Bacaro Team", "ownerId": 42, "createdAt": "2026-04-08T10:00:00Z" }
```

---

### Boards

| Method | Path | Auth | Descrizione |
|---|---|---|---|
| POST | `/api/v1/workspaces/{workspaceId}/boards` | JWT | Crea board |
| GET | `/api/v1/boards/{id}` | JWT | Dettaglio board (con liste e card) |
| PUT | `/api/v1/boards/{id}` | JWT (ADMIN) | Aggiorna board |
| DELETE | `/api/v1/boards/{id}` | JWT (ADMIN) | Elimina board |
| POST | `/api/v1/boards/{id}/members` | JWT (ADMIN) | Aggiunge membro |
| DELETE | `/api/v1/boards/{id}/members/{userId}` | JWT (ADMIN) | Rimuove membro |

**POST /boards** — request:
```json
{ "title": "Sprint Board", "color": "#0079BF" }
```
Response 201:
```json
{ "id": 1, "workspaceId": 1, "title": "Sprint Board", "color": "#0079BF", "createdAt": "2026-04-08T10:00:00Z" }
```

**GET /boards/{id}** — response 200 (board con nested lists e cards):
```json
{
  "id": 1,
  "title": "Sprint Board",
  "color": "#0079BF",
  "lists": [
    {
      "id": 1,
      "name": "To Do",
      "position": 0,
      "cards": [
        {
          "id": 1,
          "title": "Setup Ktor",
          "description": null,
          "position": 0,
          "dueDate": null,
          "assignees": [],
          "labels": [],
          "checklistCount": 0,
          "checklistDone": 0
        }
      ]
    }
  ]
}
```

---

### Lists

| Method | Path | Auth | Descrizione |
|---|---|---|---|
| POST | `/api/v1/boards/{boardId}/lists` | JWT (MEMBER+) | Crea lista |
| PUT | `/api/v1/lists/{id}` | JWT (MEMBER+) | Rinomina / riposiziona lista |
| DELETE | `/api/v1/lists/{id}` | JWT (ADMIN) | Elimina lista (cascade cards) |

**POST /boards/{boardId}/lists** — request:
```json
{ "name": "In Progress", "position": 1 }
```
Response 201:
```json
{ "id": 2, "boardId": 1, "name": "In Progress", "position": 1 }
```

---

### Cards

| Method | Path | Auth | Descrizione |
|---|---|---|---|
| POST | `/api/v1/lists/{listId}/cards` | JWT (MEMBER+) | Crea card |
| GET | `/api/v1/cards/{id}` | JWT | Dettaglio card completo |
| PUT | `/api/v1/cards/{id}` | JWT (MEMBER+) | Aggiorna card |
| DELETE | `/api/v1/cards/{id}` | JWT (MEMBER+) | Elimina card |
| PUT | `/api/v1/cards/{id}/move` | JWT (MEMBER+) | Sposta card in altra lista |
| POST | `/api/v1/cards/{id}/assignees` | JWT (MEMBER+) | Assegna utente |
| DELETE | `/api/v1/cards/{id}/assignees/{userId}` | JWT (MEMBER+) | Rimuovi assegnazione |
| POST | `/api/v1/cards/{id}/labels` | JWT (MEMBER+) | Aggiunge etichetta |
| DELETE | `/api/v1/cards/{id}/labels/{labelId}` | JWT (MEMBER+) | Rimuove etichetta |

**POST /lists/{listId}/cards** — request:
```json
{ "title": "Implement auth", "description": null, "position": 0, "dueDate": null }
```
Response 201:
```json
{ "id": 1, "listId": 1, "title": "Implement auth", "description": null, "position": 0, "dueDate": null, "createdAt": "2026-04-08T10:00:00Z" }
```

**PUT /cards/{id}/move** — request:
```json
{ "listId": 2, "position": 0 }
```
Response 200: card aggiornata.

---

### Checklist

| Method | Path | Auth | Descrizione |
|---|---|---|---|
| POST | `/api/v1/cards/{cardId}/checklist` | JWT (MEMBER+) | Aggiunge item |
| PUT | `/api/v1/cards/{cardId}/checklist/{itemId}` | JWT (MEMBER+) | Toggle isChecked / rinomina |
| DELETE | `/api/v1/cards/{cardId}/checklist/{itemId}` | JWT (MEMBER+) | Elimina item |

**POST /cards/{cardId}/checklist** — request:
```json
{ "label": "Write tests", "position": 0 }
```
Response 201:
```json
{ "id": 1, "label": "Write tests", "isChecked": false, "position": 0 }
```

---

## 7. Struttura file completa

```
bacarello-backend/
├── CLAUDE.md                                       # regole progetto per Claude Code
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── docker-compose.yml                              # PostgreSQL locale
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── application.yaml
│   │   │   ├── logback.xml
│   │   │   └── db/migration/                      # script Flyway
│   │   │       ├── V1__create_users.sql
│   │   │       ├── V2__create_workspaces_boards.sql
│   │   │       ├── V3__create_lists_cards.sql
│   │   │       └── V4__create_labels_checklist.sql
│   │   └── kotlin/com/trellodelbacaro/
│   │       ├── Application.kt
│   │       ├── domain/
│   │       │   ├── common/
│   │       │   │   └── DomainResult.kt
│   │       │   ├── model/
│   │       │   │   └── Entities.kt                # User, Workspace, Board, List, Card...
│   │       │   └── repository/
│   │       │       └── Repositories.kt            # interfacce: UserRepo, BoardRepo, CardRepo...
│   │       ├── application/
│   │       │   └── service/
│   │       │       ├── AuthService.kt             # register, login → JWT
│   │       │       ├── BoardService.kt            # orchestrazione board + members
│   │       │       ├── ListService.kt
│   │       │       └── CardService.kt             # move, assign, labels, checklist
│   │       ├── infrastructure/
│   │       │   ├── persistence/
│   │       │   │   ├── Tables.kt                  # tutti gli Exposed Table objects
│   │       │   │   ├── ExposedUserRepository.kt
│   │       │   │   ├── ExposedWorkspaceRepository.kt
│   │       │   │   ├── ExposedBoardRepository.kt
│   │       │   │   ├── ExposedListRepository.kt
│   │       │   │   └── ExposedCardRepository.kt
│   │       │   └── security/
│   │       │       └── JwtProvider.kt             # genera e valida token JWT
│   │       └── api/
│   │           ├── dto/
│   │           │   └── Dtos.kt                    # tutti i Request/Response data class
│   │           ├── plugins/
│   │           │   ├── Serialization.kt
│   │           │   ├── Authentication.kt          # configureJwt()
│   │           │   ├── Routing.kt                 # aggrega tutti i route
│   │           │   └── Database.kt                # Flyway + Exposed setup
│   │           └── routes/
│   │               ├── AuthRoutes.kt
│   │               ├── HealthRoutes.kt
│   │               ├── WorkspaceRoutes.kt
│   │               ├── BoardRoutes.kt
│   │               ├── ListRoutes.kt
│   │               └── CardRoutes.kt
│   └── test/
│       └── kotlin/com/trellodelbacaro/
│           ├── TestDatabaseFactory.kt             # H2 in-memory + Flyway migration
│           ├── TestJwtFactory.kt                  # genera token JWT per i test
│           ├── infrastructure/persistence/
│           │   ├── ExposedUserRepositoryTest.kt
│           │   ├── ExposedBoardRepositoryTest.kt
│           │   └── ExposedCardRepositoryTest.kt
│           └── api/routes/
│               ├── AuthRoutesTest.kt
│               ├── BoardRoutesTest.kt
│               └── CardRoutesTest.kt
```

---

## 8. CLAUDE.md (contenuto da mettere nella root del progetto)

```markdown
# Project: Bacarello Backend (Ktor + Kotlin)

## Stack
- Ktor 3.2.0 (Netty, routing, auth-jwt, content negotiation, websockets)
- Kotlin 2.1.10 + Coroutines
- Exposed 0.61.0 ORM + PostgreSQL (prod) / H2 (test)
- Flyway for DB migrations
- Kotest 5.9.1 + testApplication{} for integration tests
- MockK 1.13.13 for mocking
- Docker Compose for local PostgreSQL

## Architecture: 4-layer
- domain/      → pure Kotlin, zero framework deps
- application/ → use cases / services, orchestrate repositories
- infrastructure/ → Exposed repos, JwtProvider, Flyway
- api/          → Ktor routes, DTOs, plugins

## Rules
- No business logic in route handlers — always delegate to application/service
- All DB ops in suspend fun on Dispatchers.IO
- Return DomainResult<T>, never throw from services
- JWT required on all routes except /health and /api/v1/auth/**
- Error response format: { "error": "CODE", "message": "...", "details": {} }
- Timestamps: ISO 8601 UTC (kotlinx-datetime Instant)

## Domain
Workspace → Board → List → Card
Board has BoardMembers with roles: ADMIN | MEMBER | VIEWER
Cards have: labels, dueDate, assignees, checklist items

## Naming
- REST: plural nouns (/boards, /boards/{id}/lists)
- Routes: configureBoardRoutes(boardService: BoardService)
- Services: class BoardService(private val boardRepo: BoardRepository)
- Repos: class ExposedBoardRepository(private val db: Database): BoardRepository
```

---

## 9. Docker Compose (locale)

```yaml
# docker-compose.yml
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

---

## 10. Scope per 40 minuti (MVP)

Per rispettare il vincolo temporale, implementare nell'ordine:

| Priorità | Feature | Tempo |
|---|---|---|
| P0 | Scaffold + CLAUDE.md | 5 min |
| P0 | Domain layer (Entities, DomainResult, Repositories) | 4 min |
| P0 | Infrastructure/Tables + TestDatabaseFactory | 4 min |
| P0 | ExposedBoardRepository — TDD | 7 min |
| P0 | Plugins (Serialization, Database con H2) + Application.kt | 3 min |
| P0 | Board Routes CRUD — TDD | 8 min |
| P1 | ExposedCardRepository + Card Routes — TDD | 9 min |
| **P2** | **Auth (JWT), Workspace, List routes** | **fuori scope MVP** |
| **P2** | **Flyway migrations** | **fuori scope MVP** |
| **P2** | **Docker Compose prod** | **fuori scope MVP** |

> **P2 va implementato** prima di collegare il frontend reale. Il MVP in 40 min è sufficiente per sviluppo e test locali con H2.

---

## 11. Contratto OpenAPI e integrazione frontend

- **`docs/openapi.json`** — specifica OpenAPI 3.0.3 del contratto **realmente implementato** (MVP). È la fonte autoritativa per il team frontend (importabile in Swagger UI / generabile in tipi TS con `openapi-typescript`).
- **`docs/openapi-frontend-mapping.md`** — come consumare l'API dal frontend Vue e differenze rispetto allo swagger-draft creato lato frontend (`bacarello-frontend/openapi/swagger.json`): id `Long` vs `string`, radice `Workspace`, `position` intero vs `pos` float, label/checklist ancora in P2.

## 12. Avvio per il team frontend (Docker, senza Ktor/Gradle in locale)

```bash
cd bacarello-backend
docker compose up --build      # avvia app Ktor + PostgreSQL insieme
# API: http://localhost:8080  ·  health: GET /health
```

Variabili d'ambiente lette dall'app (vedi `api/plugins/Database.kt` e `docker-compose.yml`):
`DB_URL`, `DB_DRIVER`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_SIZE`, `JWT_SECRET`, `CORS_ALLOWED_HOSTS`.
Senza queste env il backend usa H2 in-memory (sviluppo locale con `./gradlew run`).
