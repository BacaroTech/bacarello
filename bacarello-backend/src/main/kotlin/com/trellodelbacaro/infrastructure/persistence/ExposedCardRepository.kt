package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Card
import com.trellodelbacaro.domain.repository.CardRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExposedCardRepository(private val database: Database) : CardRepository {

    override suspend fun findById(id: Long): DomainResult<Card> = runCatching {
        transaction(database) {
            Cards.selectAll().where { Cards.id eq id }.singleOrNull()
                ?.let { DomainResult.Success(it.toCard()) }
                ?: DomainResult.Failure.NotFound("Card $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(listId: Long, card: Card): DomainResult<Card> = runCatching {
        transaction(database) {
            val now = Clock.System.now()
            val newId = Cards.insert {
                it[Cards.listId] = listId
                it[title] = card.title
                it[description] = card.description
                it[position] = card.position
                it[dueDate] = card.dueDate
                it[createdAt] = now
            } get Cards.id
            DomainResult.Success(card.copy(id = newId, listId = listId, createdAt = now))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun update(card: Card): DomainResult<Card> = runCatching {
        transaction(database) {
            val id = card.id ?: return@transaction DomainResult.Failure.ValidationError(listOf("id required"))
            val rows = Cards.update({ Cards.id eq id }) {
                it[title] = card.title
                it[description] = card.description
                it[position] = card.position
                it[dueDate] = card.dueDate
            }
            if (rows > 0) {
                Cards.selectAll().where { Cards.id eq id }.single().let { DomainResult.Success(it.toCard()) }
            } else {
                DomainResult.Failure.NotFound("Card $id not found")
            }
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun delete(id: Long): DomainResult<Unit> = runCatching {
        transaction(database) {
            val rows = Cards.deleteWhere { Cards.id eq id }
            if (rows > 0) DomainResult.Success(Unit) else DomainResult.Failure.NotFound("Card $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun move(cardId: Long, targetListId: Long, newPosition: Int): DomainResult<Card> = runCatching {
        transaction(database) {
            val rows = Cards.update({ Cards.id eq cardId }) {
                it[listId] = targetListId
                it[position] = newPosition
            }
            if (rows > 0) {
                Cards.selectAll().where { Cards.id eq cardId }.single().let { DomainResult.Success(it.toCard()) }
            } else {
                DomainResult.Failure.NotFound("Card $cardId not found")
            }
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toCard() = Card(
        id = this[Cards.id],
        listId = this[Cards.listId],
        title = this[Cards.title],
        description = this[Cards.description],
        position = this[Cards.position],
        dueDate = this[Cards.dueDate],
        createdAt = this[Cards.createdAt]
    )
}
