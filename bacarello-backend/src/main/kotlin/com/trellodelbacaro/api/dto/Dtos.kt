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

// Workspace
@Serializable
data class WorkspaceRequest(val name: String)

@Serializable
data class WorkspaceResponse(val id: Long, val name: String, val ownerId: Long, val createdAt: Instant?)

// List
@Serializable
data class ListRequest(val name: String, val position: Int = 0)

@Serializable
data class ListResponse(val id: Long, val boardId: Long, val name: String, val position: Int)

// Card
@Serializable
data class CardRequest(val title: String, val description: String? = null, val position: Int, val dueDate: Instant? = null)

@Serializable
data class CardResponse(
    val id: Long,
    val listId: Long,
    val title: String,
    val description: String?,
    val position: Int,
    val dueDate: Instant?,
    val createdAt: Instant?
)

@Serializable
data class MoveCardRequest(val listId: Long, val position: Int)
