# Project: Bacarello Backend (Ktor + Kotlin)

Sei un Software Architect esperto in Kotlin e Ktor. Devo progettare da zero un backend "Trello-like" che serva un frontend Vue.js esistente.

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

## Test rules (lesson learned 2026-04-08)
- src/test/resources/application.yaml con ktor.development: false — SEMPRE nel scaffold
- beforeSpec crea tutta la catena FK (User → Workspace → Board → List)
- Mai ID hardcoded nei body delle request — estrarre dalla response


quando ti scrivo
"vai costruisci il backend"
devi fare questo:
"
Leggi il piano in ../docs/superpowers/plans/2026-04-08-bacarello-backend-v2.md e il documento ../docs/bacarello-backend-specs.md. Usa la skill superpowers:executing-plans (o subagent-driven-development) ed eseguilo task per task. Lavora dentro bacarello-backend/. Vai.

Voglio che tu sia totalmente autonomo e che tu non mi chieda mai precisazioni
la valutazione del tuo lavoro voglio farla solo al completamento.
Full autonomous fino alla fine. Eseguo tutti i task uno dopo l'altro, non fermarti mai prova e riprova a sistemare come meglio reputi quando i test falliscono. Tutti i fallimenti e i test vanno documentati e sistemati, se un passaggio è troppo complicato e non riesci a venirne fuori segnalo come 'TODO' e quindi lo sistemeremo, ma devi trovare il modo per andare oltre. 
I principi di clean code e clean architecture devono essere la single source of truth.
lavora in un worktree separato facendo tutti i commit task per task o anche subtask se serve.
alla fine del worktree autonomamente sposta tutti i commit in questo branch