package com.trellodelbacaro.application.service

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Workspace
import com.trellodelbacaro.domain.repository.WorkspaceRepository

class WorkspaceService(private val workspaceRepo: WorkspaceRepository) {
    suspend fun findAllByOwner(ownerId: Long): DomainResult<List<Workspace>> = workspaceRepo.findAllByOwner(ownerId)
    suspend fun findById(id: Long): DomainResult<Workspace> = workspaceRepo.findById(id)
    suspend fun create(name: String, ownerId: Long): DomainResult<Workspace> =
        workspaceRepo.create(Workspace(name = name, ownerId = ownerId))
}
