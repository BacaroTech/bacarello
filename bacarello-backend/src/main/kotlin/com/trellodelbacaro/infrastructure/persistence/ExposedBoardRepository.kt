package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import com.trellodelbacaro.domain.repository.BoardRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExposedBoardRepository(private val database: Database) : BoardRepository {

    override suspend fun findAll(): DomainResult<List<Board>> = runCatching {
        transaction(database) {
            DomainResult.Success(Boards.selectAll().map { it.toBoard() })
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun findById(id: Long): DomainResult<Board> = runCatching {
        transaction(database) {
            Boards.selectAll().where { Boards.id eq id }.singleOrNull()
                ?.let { DomainResult.Success(it.toBoard()) }
                ?: DomainResult.Failure.NotFound("Board $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(board: Board): DomainResult<Board> = runCatching {
        transaction(database) {
            val now = Clock.System.now()
            val newId = Boards.insert {
                it[workspaceId] = board.workspaceId
                it[title] = board.title
                it[color] = board.color
                it[createdAt] = now
            } get Boards.id
            DomainResult.Success(board.copy(id = newId, createdAt = now))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun update(board: Board): DomainResult<Board> = runCatching {
        transaction(database) {
            val id = board.id ?: return@transaction DomainResult.Failure.ValidationError(listOf("id required"))
            val rows = Boards.update({ Boards.id eq id }) {
                it[title] = board.title
                it[color] = board.color
            }
            if (rows > 0) DomainResult.Success(board) else DomainResult.Failure.NotFound("Board $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun delete(id: Long): DomainResult<Unit> = runCatching {
        transaction(database) {
            val rows = Boards.deleteWhere { Boards.id eq id }
            if (rows > 0) DomainResult.Success(Unit) else DomainResult.Failure.NotFound("Board $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toBoard() = Board(
        id = this[Boards.id],
        workspaceId = this[Boards.workspaceId],
        title = this[Boards.title],
        color = this[Boards.color],
        createdAt = this[Boards.createdAt]
    )
}
