import { defineStore } from "pinia"
import { ref } from "vue"
import { useUserAuth } from "./userAuth"
import { useWorkspaceStore } from "./workspace"
import type { Board, BoardDetail, CreateBoardRequest, UpdateBoardRequest, ApiError } from "@/types/api"

export interface BoardView{
    id?: string
    workspaceId?: number
    title: string
    label?: string | null
    owner?: string | null
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1').replace(/\/$/, '')
const API_V1_BASE_URL = API_BASE_URL.endsWith('/api/v1') ? API_BASE_URL : `${API_BASE_URL}/api/v1`

function mapBoardResponse(board: Board): BoardView {
    return {
        id: board.id,
        workspaceId: board.workspaceId,
        title: board.title,
        label: board.label ?? board.color ?? null,
        owner: board.owner,
    }
}

export const useBoardDataStore = defineStore('boardData', () => {
    const auth = useUserAuth()
    const workspaceStore = useWorkspaceStore()
    const isLoadingBoards = ref(false)
    const boardError = ref('')
    const homeUserBoards = ref<BoardView[]>([
        // Mock data kept for local UI reference.
        // {
        //     id: 1,
        //     title: 'Bacaro',
        //     label: 'Bacaro Test',
        // },
        // {
        //     id: 2,
        //     title: 'Bacaro2',
        //     label: 'Bacaro Prova'
        // }
    ])

    async function request<TResponse>(path: string, options: RequestInit = {}): Promise<TResponse> {
        const headers = new Headers(options.headers)
        headers.set('Content-Type', 'application/json')
        Object.entries(auth.getAuthorizationHeader()).forEach(([key, value]) => {
            headers.set(key, value)
        })

        const response = await fetch(`${API_V1_BASE_URL}${path}`, {
            ...options,
            headers,
        })

        if (!response.ok) {
            const error = (await response.json().catch(() => null)) as ApiError | null
            throw new Error(error?.message ?? `Request failed with status ${response.status}`)
        }

        if (response.status === 204) {
            return undefined as TResponse
        }

        return response.json() as Promise<TResponse>
    }

    async function getBoards(): Promise<void> {
        boardError.value = ''
        isLoadingBoards.value = true

        try {
            const boards = await request<Board[]>('/boards')
            homeUserBoards.value = boards.map(mapBoardResponse)
        } catch (error) {
            boardError.value = error instanceof Error ? error.message : 'Unable to load boards'
        } finally {
            isLoadingBoards.value = false
        }
    }

    async function createBoard(board: BoardView): Promise<BoardView> {
        boardError.value = ''
        if (!auth.userId) {
            boardError.value = 'Create or log in with a user before creating a board'
            throw new Error(boardError.value)
        }
        if (!workspaceStore.currentWorkspaceId) {
            boardError.value = 'Create a workspace before creating a board'
            throw new Error(boardError.value)
        }

        const payload: CreateBoardRequest = {
            workspaceId: workspaceStore.currentWorkspaceId,
            title: board.title.trim(),
            color: '#0079BF',
        }

        let createdBoard: BoardView

        try {
            createdBoard = mapBoardResponse(
                await request<Board>('/boards', {
                    method: 'POST',
                    body: JSON.stringify(payload),
                }),
            )
            createdBoard.label = createdBoard.label ?? board.label ?? null
        } catch (error) {
            boardError.value = error instanceof Error ? error.message : 'Unable to create board'
            throw error
        }

        homeUserBoards.value.push(createdBoard)
        return createdBoard
    }

    async function getBoard(boardId: string): Promise<BoardDetail> {
        boardError.value = ''

        try {
            return await request<BoardDetail>(`/boards/${encodeURIComponent(boardId)}`)
        } catch (error) {
            boardError.value = error instanceof Error ? error.message : 'Unable to load board'
            throw error
        }
    }

    async function updateBoard(boardId: string, payload: UpdateBoardRequest): Promise<BoardView> {
        boardError.value = ''

        try {
            const updatedBoard = mapBoardResponse(
                await request<Board>(`/boards/${encodeURIComponent(boardId)}`, {
                    method: 'PATCH',
                    body: JSON.stringify(payload),
                }),
            )
            const index = homeUserBoards.value.findIndex((board) => board.id === boardId)

            if (index >= 0) {
                homeUserBoards.value[index] = updatedBoard
            }

            return updatedBoard
        } catch (error) {
            boardError.value = error instanceof Error ? error.message : 'Unable to update board'
            throw error
        }
    }

    async function deleteBoard(boardId: string): Promise<void> {
        boardError.value = ''

        try {
            await request<void>(`/boards/${encodeURIComponent(boardId)}`, {
                method: 'DELETE',
            })
            homeUserBoards.value = homeUserBoards.value.filter((board) => board.id !== boardId)
        } catch (error) {
            boardError.value = error instanceof Error ? error.message : 'Unable to delete board'
            throw error
        }
    }

    return {
        homeUserBoards,
        isLoadingBoards,
        boardError,
        getBoards,
        createBoard,
        getBoard,
        updateBoard,
        deleteBoard,
    }
})
