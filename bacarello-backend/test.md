# Bacarello Backend — test manuale (curl)

Flusso end-to-end: **register → login → workspace → board → list → card** + i relativi GET.
Tutti i comandi sono stati eseguiti e verificati. Base URL: `http://localhost:8080`.

## Prerequisito: avviare il backend

```bash
# Opzione A — stack completo con PostgreSQL (consigliata per il team frontend)
docker compose up --build

# Opzione B — locale con H2 in-memory (serve JDK 21)
./gradlew run
```

Verifica che sia su:
```bash
curl -s http://localhost:8080/health
# {"status":"ok"}
```

---

## Flusso completo (copia-incolla)

> Usa variabili di shell per concatenare le chiamate. `sed` estrae token e id (niente dipendenza da `jq`).

```bash
B=http://localhost:8080
```

### 1) Register
```bash
curl -s -X POST $B/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"mario@bacaro.it","displayName":"Mario","password":"secret123"}'
```
```json
{ "id": 1, "email": "mario@bacaro.it", "displayName": "Mario", "createdAt": "2026-05-30T14:50:05.505129Z" }
```

### 2) Login → salva il token JWT
```bash
TOKEN=$(curl -s -X POST $B/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"mario@bacaro.it","password":"secret123"}' \
  | sed -n 's/.*"token"[ ]*:[ ]*"\([^"]*\)".*/\1/p')
echo "$TOKEN"
```
Da qui in poi ogni chiamata usa `-H "Authorization: Bearer $TOKEN"`.

### 3) Create workspace (l'owner è preso dal token)
```bash
WS_ID=$(curl -s -X POST $B/api/v1/workspaces \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Team Bacaro"}' \
  | sed -n 's/.*"id"[ ]*:[ ]*\([0-9]*\).*/\1/p')
echo "workspace id = $WS_ID"
```
```json
{ "id": 1, "name": "Team Bacaro", "ownerId": 1, "createdAt": "2026-05-30T14:50:05.772978Z" }
```

### 4) Create board (richiede workspaceId)
```bash
BOARD_ID=$(curl -s -X POST $B/api/v1/boards \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"workspaceId\":$WS_ID,\"title\":\"Sprint 1\",\"color\":\"#0079BF\"}" \
  | sed -n 's/.*"id"[ ]*:[ ]*\([0-9]*\).*/\1/p')
echo "board id = $BOARD_ID"
```
```json
{ "id": 1, "workspaceId": 1, "title": "Sprint 1", "color": "#0079BF", "createdAt": "..." }
```

### 5) Create list (nel board)
```bash
LIST_ID=$(curl -s -X POST $B/api/v1/boards/$BOARD_ID/lists \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"To Do","position":0}' \
  | sed -n 's/.*"id"[ ]*:[ ]*\([0-9]*\).*/\1/p')
echo "list id = $LIST_ID"
```
```json
{ "id": 1, "boardId": 1, "name": "To Do", "position": 0 }
```

### 6) Create card (nella list)
```bash
curl -s -X POST $B/api/v1/lists/$LIST_ID/cards \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Configurare CI","description":"setup pipeline","position":0}'
```
```json
{ "id": 1, "listId": 1, "title": "Configurare CI", "description": "setup pipeline", "position": 0, "dueDate": null, "createdAt": "..." }
```

### 7) GET di verifica
```bash
# tutte le board
curl -s $B/api/v1/boards -H "Authorization: Bearer $TOKEN"

# le list di un board
curl -s $B/api/v1/boards/$BOARD_ID/lists -H "Authorization: Bearer $TOKEN"

# le card di una list
curl -s $B/api/v1/lists/$LIST_ID/cards -H "Authorization: Bearer $TOKEN"

# dettaglio singola card
curl -s $B/api/v1/cards/1 -H "Authorization: Bearer $TOKEN"
```

---

## Extra utili

```bash
# Spostare una card in un'altra lista / posizione
curl -s -X PUT $B/api/v1/cards/1/move \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"listId":1,"position":1}'

# Aggiornare una card
curl -s -X PUT $B/api/v1/cards/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Configurare CI/CD","description":null,"position":0}'

# Eliminare board / card (204 No Content)
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $B/api/v1/cards/1 -H "Authorization: Bearer $TOKEN"
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $B/api/v1/boards/1 -H "Authorization: Bearer $TOKEN"
```

## Errori attesi (sanity check sull'auth)
```bash
# Senza token → 401
curl -s -o /dev/null -w "%{http_code}\n" $B/api/v1/boards            # 401

# Board inesistente → 404
curl -s -o /dev/null -w "%{http_code}\n" $B/api/v1/boards/9999 -H "Authorization: Bearer $TOKEN"  # 404
```

> Contratto completo: `docs/openapi.json` · mapping verso il frontend: `docs/openapi-frontend-mapping.md`.
