package com.trellodelbacaro.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val displayName = varchar("display_name", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Workspaces : Table("workspaces") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val ownerId = long("owner_id").references(Users.id)
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Boards : Table("boards") {
    val id = long("id").autoIncrement()
    val workspaceId = long("workspace_id").references(Workspaces.id)
    val title = varchar("title", 255)
    val color = varchar("color", 7).default("#0079BF")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object BoardMembers : Table("board_members") {
    val boardId = long("board_id").references(Boards.id)
    val userId = long("user_id").references(Users.id)
    val role = varchar("role", 20).default("MEMBER")
    override val primaryKey = PrimaryKey(boardId, userId)
}

object Lists : Table("lists") {
    val id = long("id").autoIncrement()
    val boardId = long("board_id").references(Boards.id)
    val name = varchar("name", 255)
    val position = integer("position")
    override val primaryKey = PrimaryKey(id)
}

object Cards : Table("cards") {
    val id = long("id").autoIncrement()
    val listId = long("list_id").references(Lists.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val position = integer("position")
    val dueDate = timestamp("due_date").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ChecklistItems : Table("checklist_items") {
    val id = long("id").autoIncrement()
    val cardId = long("card_id").references(Cards.id)
    val label = varchar("label", 255)
    val isChecked = bool("is_checked").default(false)
    val position = integer("position")
    override val primaryKey = PrimaryKey(id)
}
