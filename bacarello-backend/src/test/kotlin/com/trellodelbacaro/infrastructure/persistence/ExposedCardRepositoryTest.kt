package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Card
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedCardRepositoryTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val repo = ExposedCardRepository(db)
    var listId: Long = -1L
    var boardId: Long = -1L

    beforeSpec {
        transaction(db) {
            val now = Clock.System.now()
            val userId = Users.insert {
                it[email] = "card@bacaro.it"; it[displayName] = "T"; it[passwordHash] = "h"; it[createdAt] = now
            } get Users.id
            val wsId = Workspaces.insert {
                it[name] = "WS"; it[ownerId] = userId; it[createdAt] = now
            } get Workspaces.id
            boardId = Boards.insert {
                it[workspaceId] = wsId; it[title] = "B"; it[color] = "#000"; it[createdAt] = now
            } get Boards.id
            listId = Lists.insert {
                it[Lists.boardId] = boardId; it[name] = "To Do"; it[position] = 0
            } get Lists.id
        }
    }

    "create should return card with generated id" {
        val result = repo.create(listId, Card(listId = listId, title = "Spritz card", position = 0))
        result.shouldBeInstanceOf<DomainResult.Success<Card>>()
        result.value.title shouldBe "Spritz card"
    }

    "findById should return the created card" {
        val created = (repo.create(listId, Card(listId = listId, title = "Baccalà", position = 1)) as DomainResult.Success).value
        repo.findById(created.id!!).shouldBeInstanceOf<DomainResult.Success<Card>>()
    }

    "findById should return NotFound for unknown id" {
        repo.findById(9999L).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "update should modify the card title" {
        val created = (repo.create(listId, Card(listId = listId, title = "Old", position = 2)) as DomainResult.Success).value
        val result = repo.update(created.copy(title = "New"))
        result.shouldBeInstanceOf<DomainResult.Success<Card>>()
        result.value.title shouldBe "New"
    }

    "delete should remove the card" {
        val created = (repo.create(listId, Card(listId = listId, title = "ToDelete", position = 3)) as DomainResult.Success).value
        repo.delete(created.id!!).shouldBeInstanceOf<DomainResult.Success<Unit>>()
        repo.findById(created.id!!).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "move should change listId and position" {
        val created = (repo.create(listId, Card(listId = listId, title = "Moving", position = 0)) as DomainResult.Success).value
        val list2Id = transaction(db) {
            Lists.insert { it[Lists.boardId] = boardId; it[name] = "Done"; it[position] = 1 } get Lists.id
        }
        val moved = repo.move(created.id!!, list2Id, 0)
        moved.shouldBeInstanceOf<DomainResult.Success<Card>>()
        moved.value.listId shouldBe list2Id
    }
})
