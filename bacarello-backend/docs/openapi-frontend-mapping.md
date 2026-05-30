# Mapping API backend ↔ frontend Vue

> Come consumare `docs/openapi.json` (contratto reale del backend) dal frontend `bacarello-frontend`,
> e differenze rispetto allo swagger-draft creato lato frontend (`bacarello-frontend/openapi/swagger.json`).

## 1. Dove sta la verità

`docs/openapi.json` è il **contratto autoritativo**: descrive gli endpoint e i DTO effettivamente
serviti dal backend Ktor (MVP). Lo swagger-draft del frontend era un'esplorazione fatta dal
modello degli store Vue e usava convenzioni diverse — qui sotto le divergenze e come riconciliarle.

## 2. Differenze chiave (draft frontend → backend reale)

| Aspetto | Draft frontend | Backend reale (openapi.json) |
|---|---|---|
| Tipo id | `string` (stile Trello) | `Long` / `integer(int64)` |
| Radice gerarchia | Board come radice | `Workspace → Board → List → Card` (la board richiede `workspaceId`) |
| Liste | `Column` | `List` (id, boardId, name, position) |
| Card | `Task` | `Card` |
| `Task.endTask` | stringa data | `Card.dueDate` (date-time ISO-8601) |
| `Task.labels` | `string[]` di id label | **non ancora nel MVP** (label = P2) |
| Label | board-scoped riusabili | **non ancora nel MVP** (P2) |
| Checklist | multiple titolate | tabella `checklist_items` per-card, **route P2** |
| Posizione | float `pos` (stile Trello) | `position` intero index-based |

## 3. Mapping dei tipi TS del frontend

I tipi in `bacarello-frontend/src/types/api.ts` vanno adattati così per parlare col backend reale:

```ts
// Board (frontend BoardView) ── backend BoardResponse
interface BoardResponse { id: number; workspaceId: number; title: string; color: string; createdAt?: string }
// NB: il frontend usa `label`; il backend usa `color`. `title` resta `title`.

// List (frontend Column) ── backend (creata via POST /boards/{id}/lists in P2)
interface ListResponse { id: number; boardId: number; name: string; position: number }

// Card (frontend Task) ── backend CardResponse
interface CardResponse {
  id: number; listId: number; title: string;
  description?: string | null; position: number;
  dueDate?: string | null;        // era Task.endTask
  createdAt?: string | null;
}
```

## 4. Flusso di autenticazione (obbligatorio)

Tutte le route tranne `/health` e `/api/v1/auth/**` richiedono `Authorization: Bearer <jwt>`.

```ts
// 1) register
await fetch('/api/v1/auth/register', { method:'POST', headers:{'Content-Type':'application/json'},
  body: JSON.stringify({ email, displayName, password }) })
// 2) login → { token, expiresAt }
const { token } = await (await fetch('/api/v1/auth/login', { method:'POST', ... })).json()
// 3) usa il token su ogni chiamata
await fetch('/api/v1/boards', { headers: { Authorization: `Bearer ${token}` } })
```

## 5. Generare i tipi dal contratto reale

Invece di mantenerli a mano, si possono rigenerare dallo OpenAPI:

```bash
# dal frontend
npx openapi-typescript ../bacarello-backend/docs/openapi.json -o src/types/api.gen.ts
```

## 6. CORS

Il backend abilita CORS per le origini in `CORS_ALLOWED_HOSTS` (vedi `docker-compose.yml`:
`localhost:5173` dev Vite, `localhost:4173` preview). In sviluppo locale senza quella env, accetta
qualsiasi origine.

## 7. Endpoint ancora da implementare (P2, vedi backend-plan.md)

Workspace routes, List routes, Board members, Label, Checklist routes, Flyway. Lo swagger-draft del
frontend li prevede già: quando verranno implementati, `openapi.json` sarà esteso di conseguenza.
