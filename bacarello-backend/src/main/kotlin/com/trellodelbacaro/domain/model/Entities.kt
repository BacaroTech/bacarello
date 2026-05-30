package com.trellodelbacaro.domain.model

import kotlinx.datetime.Instant

data class User(
    val id: Long? = null,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val createdAt: Instant? = null
)

enum class BoardRole { ADMIN, MEMBER, VIEWER }

data class Workspace(
    val id: Long? = null,
    val name: String,
    val ownerId: Long,
    val createdAt: Instant? = null
)

data class Board(
    val id: Long? = null,
    val workspaceId: Long,
    val title: String,
    val color: String = "#0079BF",
    val createdAt: Instant? = null,
    val lists: List<BoardList> = emptyList()
)

data class BoardMember(
    val boardId: Long,
    val userId: Long,
    val role: BoardRole
)

data class BoardList(
    val id: Long? = null,
    val boardId: Long,
    val name: String,
    val position: Int,
    val cards: List<Card> = emptyList()
)

data class Card(
    val id: Long? = null,
    val listId: Long,
    val title: String,
    val description: String? = null,
    val position: Int,
    val dueDate: Instant? = null,
    val createdAt: Instant? = null,
    val checklist: List<ChecklistItem> = emptyList()
)

data class ChecklistItem(
    val id: Long? = null,
    val cardId: Long,
    val label: String,
    val isChecked: Boolean = false,
    val position: Int
)
