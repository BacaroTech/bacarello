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
