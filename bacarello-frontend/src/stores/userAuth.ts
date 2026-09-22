import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const TOKEN_STORAGE_KEY = 'bacarello.auth.token'
const EXPIRES_AT_STORAGE_KEY = 'bacarello.auth.expiresAt'
const USER_STORAGE_KEY = 'bacarello.auth.user'
const WORKSPACE_ID_STORAGE_KEY = 'bacarello.workspace.id'

export interface RegisterRequest {
  email: string
  displayName: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UserResponse {
  id: number
  email: string
  displayName: string
  createdAt: string
}

export interface LoginResponse {
  token: string
  expiresAt: string
}

interface ErrorResponse {
  error: string
  message: string
}

async function postJson<TResponse, TBody>(path: string, body: TBody): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ErrorResponse | null
    throw new Error(error?.message ?? `Request failed with status ${response.status}`)
  }

  return response.json() as Promise<TResponse>
}

function readStoredUser(): UserResponse | null {
  const storedUser = localStorage.getItem(USER_STORAGE_KEY)
  return storedUser ? (JSON.parse(storedUser) as UserResponse) : null
}

export const useUserAuth = defineStore('userAuth', () => {
  const token = ref(localStorage.getItem(TOKEN_STORAGE_KEY))
  const expiresAt = ref(localStorage.getItem(EXPIRES_AT_STORAGE_KEY))
  const user = ref<UserResponse | null>(readStoredUser())

  const isAuthenticated = computed(() => {
    if (!token.value || !expiresAt.value) return false

    return new Date(expiresAt.value).getTime() > Date.now()
  })
  const userId = computed(() => user.value?.id ?? null)

  async function registerUser(payload: RegisterRequest): Promise<UserResponse> {
    const registeredUser = await postJson<UserResponse, RegisterRequest>('/api/v1/auth/register', payload)
    user.value = registeredUser
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(registeredUser))

    return registeredUser
  }

  async function loginUser(payload: LoginRequest): Promise<LoginResponse> {
    const session = await postJson<LoginResponse, LoginRequest>('/api/v1/auth/login', payload)

    token.value = session.token
    expiresAt.value = session.expiresAt
    localStorage.setItem(TOKEN_STORAGE_KEY, session.token)
    localStorage.setItem(EXPIRES_AT_STORAGE_KEY, session.expiresAt)

    return session
  }

  async function registerAndLoginUser(payload: RegisterRequest): Promise<UserResponse> {
    const registeredUser = await registerUser(payload)
    await loginUser({ email: payload.email, password: payload.password })

    return registeredUser
  }

  function getAuthorizationHeader(): Record<string, string> {
    return token.value ? { Authorization: `Bearer ${token.value}` } : {}
  }

  function logoutUser() {
    token.value = null
    expiresAt.value = null
    user.value = null
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    localStorage.removeItem(EXPIRES_AT_STORAGE_KEY)
    localStorage.removeItem(USER_STORAGE_KEY)
    localStorage.removeItem(WORKSPACE_ID_STORAGE_KEY)
  }

  function checkAuthentication(): boolean {
    if (isAuthenticated.value) return true

    logoutUser()
    return false
  }

  return {
    token,
    expiresAt,
    user,
    userId,
    isAuthenticated,
    registerUser,
    loginUser,
    registerAndLoginUser,
    getAuthorizationHeader,
    logoutUser,
    checkAuthentication,
  }
})
