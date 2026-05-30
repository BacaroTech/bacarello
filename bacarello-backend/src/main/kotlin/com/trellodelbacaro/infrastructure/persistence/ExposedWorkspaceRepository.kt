package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Workspace
import com.trellodelbacaro.domain.repository.WorkspaceRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedWorkspaceRepository(private val database: Database) : WorkspaceRepository {

    override suspend fun findAllByOwner(ownerId: Long): DomainResult<List<Workspace>> = runCatching {
        transaction(database) {
            DomainResult.Success(
                Workspaces.selectAll().where { Workspaces.ownerId eq ownerId }.map { it.toWorkspace() }
            )
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun findById(id: Long): DomainResult<Workspace> = runCatching {
        transaction(database) {
            Workspaces.selectAll().where { Workspaces.id eq id }.singleOrNull()
                ?.let { DomainResult.Success(it.toWorkspace()) }
                ?: DomainResult.Failure.NotFound("Workspace $id not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(workspace: Workspace): DomainResult<Workspace> = runCatching {
        transaction(database) {
            val now = Clock.System.now()
            val newId = Workspaces.insert {
                it[name] = workspace.name
                it[ownerId] = workspace.ownerId
                it[createdAt] = now
            } get Workspaces.id
            DomainResult.Success(workspace.copy(id = newId, createdAt = now))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toWorkspace() = Workspace(
        id = this[Workspaces.id],
        name = this[Workspaces.name],
        ownerId = this[Workspaces.ownerId],
        createdAt = this[Workspaces.createdAt]
    )
}
