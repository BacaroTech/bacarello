package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.BoardList
import com.trellodelbacaro.domain.repository.ListRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedListRepository(private val database: Database) : ListRepository {

    override suspend fun findByBoard(boardId: Long): DomainResult<List<BoardList>> = runCatching {
        transaction(database) {
            DomainResult.Success(
                Lists.selectAll().where { Lists.boardId eq boardId }
                    .orderBy(Lists.position to SortOrder.ASC)
                    .map { it.toBoardList() }
            )
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun findById(id: Long): DomainResult<BoardList> = runCatching {
        transaction(database) {
            Lists.selectAll().where { Lists.id eq id }.singleOrNull()
                ?.let { DomainResult.Success(it.toBoardList()) }
                ?: DomainResult.Failure.NotFound("List $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(list: BoardList): DomainResult<BoardList> = runCatching {
        transaction(database) {
            val newId = Lists.insert {
                it[boardId] = list.boardId
                it[name] = list.name
                it[position] = list.position
            } get Lists.id
            DomainResult.Success(list.copy(id = newId))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toBoardList() = BoardList(
        id = this[Lists.id],
        boardId = this[Lists.boardId],
        name = this[Lists.name],
        position = this[Lists.position]
    )
}
