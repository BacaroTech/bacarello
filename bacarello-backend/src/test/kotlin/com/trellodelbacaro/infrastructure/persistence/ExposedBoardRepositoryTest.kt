package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedBoardRepositoryTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val repo = ExposedBoardRepository(db)
    var workspaceId: Long = -1L

    beforeSpec {
        transaction(db) {
            val now = Clock.System.now()
            val userId = Users.insert {
                it[email] = "test@bacaro.it"
                it[displayName] = "Tester"
                it[passwordHash] = "hash"
                it[createdAt] = now
            } get Users.id
            workspaceId = Workspaces.insert {
                it[name] = "Test Workspace"
                it[ownerId] = userId
                it[createdAt] = now
            } get Workspaces.id
        }
    }

    "create should return board with generated id" {
        val result = repo.create(Board(workspaceId = workspaceId, title = "Il Bacaro", color = "#0079BF"))
        result.shouldBeInstanceOf<DomainResult.Success<Board>>()
        result.value.title shouldBe "Il Bacaro"
        result.value.id shouldBe 1L
    }

    "findById should return the created board" {
        val created = (repo.create(Board(workspaceId = workspaceId, title = "Spritz", color = "#FF6347")) as DomainResult.Success).value
        val result = repo.findById(created.id!!)
        result.shouldBeInstanceOf<DomainResult.Success<Board>>()
        result.value.title shouldBe "Spritz"
    }

    "findById should return NotFound for unknown id" {
        val result = repo.findById(9999L)
        result.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "findAll should return all created boards" {
        val result = repo.findAll()
        result.shouldBeInstanceOf<DomainResult.Success<List<Board>>>()
        (result.value.size >= 1) shouldBe true
    }

    "update should modify the board title" {
        val created = (repo.create(Board(workspaceId = workspaceId, title = "Old", color = "#000")) as DomainResult.Success).value
        val updated = repo.update(created.copy(title = "New"))
        updated.shouldBeInstanceOf<DomainResult.Success<Board>>()
        updated.value.title shouldBe "New"
    }

    "update should return NotFound for unknown id" {
        val result = repo.update(Board(id = 9999L, workspaceId = workspaceId, title = "X", color = "#000"))
        result.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "delete should remove the board" {
        val created = (repo.create(Board(workspaceId = workspaceId, title = "ToDelete", color = "#ccc")) as DomainResult.Success).value
        repo.delete(created.id!!).shouldBeInstanceOf<DomainResult.Success<Unit>>()
        repo.findById(created.id!!).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "delete should return NotFound for unknown id" {
        repo.delete(9999L).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }
})
