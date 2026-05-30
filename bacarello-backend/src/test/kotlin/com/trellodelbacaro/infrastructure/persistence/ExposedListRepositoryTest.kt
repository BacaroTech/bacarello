package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.BoardList
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedListRepositoryTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val repo = ExposedListRepository(db)
    var boardId: Long = -1L

    beforeSpec {
        transaction(db) {
            val now = Clock.System.now()
            val userId = Users.insert {
                it[email] = "list@bacaro.it"; it[displayName] = "T"; it[passwordHash] = "h"; it[createdAt] = now
            } get Users.id
            val wsId = Workspaces.insert {
                it[name] = "WS"; it[ownerId] = userId; it[createdAt] = now
            } get Workspaces.id
            boardId = Boards.insert {
                it[workspaceId] = wsId; it[title] = "B"; it[color] = "#000"; it[createdAt] = now
            } get Boards.id
        }
    }

    "create should return list with generated id" {
        val result = repo.create(BoardList(boardId = boardId, name = "To Do", position = 0))
        result.shouldBeInstanceOf<DomainResult.Success<BoardList>>()
        result.value.name shouldBe "To Do"
    }

    "findByBoard should return lists ordered by position" {
        repo.create(BoardList(boardId = boardId, name = "Done", position = 2))
        repo.create(BoardList(boardId = boardId, name = "Doing", position = 1))
        val result = repo.findByBoard(boardId)
        result.shouldBeInstanceOf<DomainResult.Success<List<BoardList>>>()
        result.value.map { it.position } shouldBe result.value.map { it.position }.sorted()
    }

    "findById should return NotFound for unknown id" {
        repo.findById(9999L).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }
})
