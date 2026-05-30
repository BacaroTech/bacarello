/**
 * Tipi del contratto API di bacarello.
 * Allineati a openapi/swagger.json — modello del frontend (Board → List → Card),
 * ispirato per i campi di create/move alle catture in json-example.
 *
 * Convenzioni:
 * - Tutti gli id sono `string` (normalizzati: il frontend aveva BoardView.id:number
 *   ma Column/Task.id:string — qui uniformiamo a string, stile Trello).
 * - I timestamp sono ISO-8601 UTC (`string`), es. "2026-02-20T00:00:00Z".
 * - `null` = valore assente dal server; `?` = campo che può non essere inviato.
 */

// ────────────────────────────── Entità ──────────────────────────────

export interface Board {
  id: string
  title: string
  /** Sottotitolo/etichetta libera (frontend BoardView.label). */
  label?: string | null
  /** Id utente proprietario. */
  owner?: string | null
}

export interface BoardDetail extends Board {
  lists: ListWithCards[]
  labels: Label[]
}

export interface List {
  id: string
  boardId: string
  name: string
  /** Posizione ordinale (float, stile Trello). */
  pos?: number
  /** Lista archiviata. */
  closed?: boolean
}

export interface ListWithCards extends List {
  cards: Card[]
}

export interface Card {
  id: string
  listId: string
  title: string
  description?: string | null
  /** Riferimenti alle Label board-scoped (frontend Task.labels: string[]). */
  labelIds?: string[]
  completed?: boolean
  /** Scadenza ISO-8601 UTC (frontend Task.endTask). */
  dueDate?: string | null
  /** Scadenza segnata come completata (json-example: dueComplete). */
  dueComplete?: boolean
  assigneeIds?: string[]
  pos?: number
}

export interface CardDetail extends Card {
  /** Label risolte in oggetti completi (non solo id). */
  labels: Label[]
  checklists: Checklist[]
}

export interface Label {
  id: string
  /** Può essere vuota (label di solo colore). */
  name?: string
  /** Classe colore Tailwind (frontend LabelItem.color), es. "bg-green-500". */
  color: string
}

export interface Checklist {
  id: string
  /** Titolo della checklist (frontend CheckList.label). */
  label: string
  items: ChecklistItem[]
}

export interface ChecklistItem {
  id: string
  label: string
  checked?: boolean
}

// ───────────────────────── Request bodies ──────────────────────────

export interface CreateBoardRequest {
  title: string
  label?: string | null
}

export type UpdateBoardRequest = Partial<Pick<Board, 'title' | 'label'>>

export interface CreateListRequest {
  name: string
  /** Opzionale; se omesso il backend appende in coda. */
  pos?: number
}

export interface UpdateListRequest {
  name?: string
  pos?: number
  closed?: boolean
}

export interface CreateCardRequest {
  title: string
  description?: string | null
  pos?: number
  dueDate?: string | null
  labelIds?: string[]
}

export interface UpdateCardRequest {
  title?: string
  description?: string | null
  completed?: boolean
  dueDate?: string | null
  dueComplete?: boolean
  labelIds?: string[]
  assigneeIds?: string[]
}

/**
 * TODO (decisione di design — tua):
 * Definisci la strategia di posizionamento per lo spostamento card.
 *
 * Due approcci validi:
 *
 *  (A) FLOAT POS — come Trello / json-example (jsonChangeList.json: idList + pos).
 *      Il client calcola pos = (posPrima + posDopo) / 2 e lo invia. Niente
 *      reindicizzazione lato server, ma serve gestire la "ribilanciatura" quando
 *      i float si avvicinano troppo.
 *        { listId?: string; pos?: number }
 *
 *  (B) INDEX-BASED — il client manda l'indice di destinazione (0,1,2...) e il
 *      server riordina la lista. Più semplice da ragionare con vuedraggable
 *      (che già lavora per indici), ma scritture O(n) sulla lista.
 *        { listId?: string; index?: number }
 *
 * Lo swagger usa di default (A). Scegli e completa l'interfaccia qui sotto;
 * se cambi approccio, aggiorna anche MoveCardRequest in openapi/swagger.json.
 */
export interface MoveCardRequest {
  /** Lista di destinazione; se omessa la card resta nella stessa lista. */
  listId?: string
  // TODO: aggiungi qui il campo posizione secondo l'approccio scelto (pos?: number | index?: number)
}

export interface CreateLabelRequest {
  name?: string
  color: string
}

export interface CreateChecklistRequest {
  label: string
}

export interface CreateChecklistItemRequest {
  label: string
}

export interface UpdateChecklistItemRequest {
  label?: string
  checked?: boolean
}

// ───────────────────────────── Errori ──────────────────────────────

/** Forma di errore canonica del progetto (vedi CLAUDE.md). */
export interface ApiError {
  error: string
  message: string
  details?: Record<string, unknown>
}
