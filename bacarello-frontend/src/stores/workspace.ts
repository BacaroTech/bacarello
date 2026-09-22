import { defineStore } from "pinia"
import { ref } from "vue"
import { useUserAuth } from "./userAuth"
import type { ApiError, CreateWorkspaceRequest, Workspace } from "@/types/api"

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1').replace(/\/$/, '')
const API_V1_BASE_URL = API_BASE_URL.endsWith('/api/v1') ? API_BASE_URL : `${API_BASE_URL}/api/v1`
const WORKSPACE_ID_STORAGE_KEY = 'bacarello.workspace.id'

export const useWorkspaceStore = defineStore('workspaceData', () => {
  const auth = useUserAuth()
  const workspaces = ref<Workspace[]>([])
  const currentWorkspaceId = ref<number | null>(
    Number(localStorage.getItem(WORKSPACE_ID_STORAGE_KEY)) || null,
  )
  const workspaceError = ref('')
  const isCreatingWorkspace = ref(false)

  function setCurrentWorkspace(workspace: Workspace) {
    currentWorkspaceId.value = workspace.id
    localStorage.setItem(WORKSPACE_ID_STORAGE_KEY, String(workspace.id))
  }

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

  async function createWorkspace(name = 'Team Bacaro'): Promise<Workspace> {
    workspaceError.value = ''
    isCreatingWorkspace.value = true

    const payload: CreateWorkspaceRequest = {
      name,
    }

    try {
      const workspace = await request<Workspace>('/workspaces', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      workspaces.value.push(workspace)
      setCurrentWorkspace(workspace)
      return workspace
    } catch (error) {
      workspaceError.value = error instanceof Error ? error.message : 'Unable to create workspace'
      throw error
    } finally {
      isCreatingWorkspace.value = false
    }
  }

  return {
    workspaces,
    currentWorkspaceId,
    workspaceError,
    isCreatingWorkspace,
    setCurrentWorkspace,
    createWorkspace,
  }
})
