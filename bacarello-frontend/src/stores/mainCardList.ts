import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useUserAuth } from './userAuth'
import type { ApiError, CreateListRequest, List } from '@/types/api'
export interface Task {
  id: string
  title: string
  description?: string
  labels?: string[]
  endTask?: string
  completed?: boolean
}

export interface Column {
  id: string
  name: string
  tasks: Task[]
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1').replace(/\/$/, '')
const API_V1_BASE_URL = API_BASE_URL.endsWith('/api/v1') ? API_BASE_URL : `${API_BASE_URL}/api/v1`

function mapListResponse(list: List): Column {
  return {
    id: list.id,
    name: list.name,
    tasks: [],
  }
}

export const useTrelloList = defineStore('trelloList', () => {
  const auth = useUserAuth()
  const isLoadingLists = ref(false)
  const listError = ref('')
  const trelloMock = ref<Column[]>([
    {
      id: 'todo',
      name: 'To Do',
      tasks: [
        {
          id: 'task-1',
          title: 'Design login page',
          description: 'Create responsive UI for login',
          labels: ['design', 'ui'],
          endTask: '2026-02-20',
        },
        {
          id: 'task-2',
          title: 'Setup Pinia store',
          labels: ['backend'],
        },
      ],
    },
    {
      id: 'in-progress',
      name: 'In Progress',
      tasks: [
        {
          id: 'task-3',
          title: 'Implement drag & drop',
          labels: ['feature'],
          endTask: '2026-02-18',
        },
      ],
    },
    {
      id: 'done',
      name: 'Done',
      tasks: [
        {
          id: 'task-4',
          title: 'Project scaffolding',
          labels: ['setup'],
        },
      ],
    },
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

    return response.json() as Promise<TResponse>
  }

  async function getLists(boardId: string): Promise<void> {
    listError.value = ''
    isLoadingLists.value = true

    try {
      const lists = await request<List[]>(`/boards/${encodeURIComponent(boardId)}/lists`)
      trelloMock.value = lists.map(mapListResponse)
    } catch (error) {
      listError.value = error instanceof Error ? error.message : 'Unable to load lists'
    } finally {
      isLoadingLists.value = false
    }
  }

  function handleAddTask({ columnId, task }: { columnId: string; task: Task }) {
    const column = trelloMock.value.find((c) => c.id === columnId)
    if (!column) return
    column.tasks.push(task)
  }

  async function addColumn(boardId: string, { name }: { id: string; name: string }): Promise<void> {
    listError.value = ''
    const payload: CreateListRequest = { name }

    try {
      const list = await request<List>(`/boards/${encodeURIComponent(boardId)}/lists`, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      trelloMock.value.push(mapListResponse(list))
    } catch (error) {
      listError.value = error instanceof Error ? error.message : 'Unable to create list'
      throw error
    }
  }

  return {trelloMock, isLoadingLists, listError, getLists, handleAddTask, addColumn}
})
