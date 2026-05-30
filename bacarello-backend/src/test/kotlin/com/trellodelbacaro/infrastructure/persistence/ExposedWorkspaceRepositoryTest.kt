package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.TestDatabaseFactory
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Workspace
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedWorkspaceRepositoryTest : StringSpec({

    val db = TestDatabaseFactory.create()
    val repo = ExposedWorkspaceRepository(db)
    var ownerId: Long = -1L

    beforeSpec {
        transaction(db) {
            ownerId = Users.insert {
                it[email] = "ws@bacaro.it"; it[displayName] = "Owner"; it[passwordHash] = "h"; it[createdAt] = Clock.System.now()
            } get Users.id
        }
    }

    "create should return workspace with generated id" {
        val result = repo.create(Workspace(name = "Team Bacaro", ownerId = ownerId))
        result.shouldBeInstanceOf<DomainResult.Success<Workspace>>()
        result.value.name shouldBe "Team Bacaro"
        result.value.id shouldBe 1L
    }

    "findById should return the created workspace" {
        val created = (repo.create(Workspace(name = "WS2", ownerId = ownerId)) as DomainResult.Success).value
        repo.findById(created.id!!).shouldBeInstanceOf<DomainResult.Success<Workspace>>()
    }

    "findById should return NotFound for unknown id" {
        repo.findById(9999L).shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    "findAllByOwner should return only owner's workspaces" {
        val result = repo.findAllByOwner(ownerId)
        result.shouldBeInstanceOf<DomainResult.Success<List<Workspace>>>()
        result.value.all { it.ownerId == ownerId } shouldBe true
    }
})
