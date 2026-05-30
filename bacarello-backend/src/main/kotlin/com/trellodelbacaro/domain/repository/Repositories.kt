package com.trellodelbacaro.domain.repository

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import com.trellodelbacaro.domain.model.BoardList
import com.trellodelbacaro.domain.model.Card
import com.trellodelbacaro.domain.model.User
import com.trellodelbacaro.domain.model.Workspace

interface UserRepository {
    suspend fun findByEmail(email: String): DomainResult<User>
    suspend fun create(user: User): DomainResult<User>
}

interface WorkspaceRepository {
    suspend fun findAllByOwner(ownerId: Long): DomainResult<List<Workspace>>
    suspend fun findById(id: Long): DomainResult<Workspace>
    suspend fun create(workspace: Workspace): DomainResult<Workspace>
}

interface BoardRepository {
    suspend fun findAll(): DomainResult<List<Board>>
    suspend fun findById(id: Long): DomainResult<Board>
    suspend fun create(board: Board): DomainResult<Board>
    suspend fun update(board: Board): DomainResult<Board>
    suspend fun delete(id: Long): DomainResult<Unit>
}

interface ListRepository {
    suspend fun findByBoard(boardId: Long): DomainResult<List<BoardList>>
    suspend fun findById(id: Long): DomainResult<BoardList>
    suspend fun create(list: BoardList): DomainResult<BoardList>
}

interface CardRepository {
    suspend fun findById(id: Long): DomainResult<Card>
    suspend fun findByList(listId: Long): DomainResult<List<Card>>
    suspend fun create(listId: Long, card: Card): DomainResult<Card>
    suspend fun update(card: Card): DomainResult<Card>
    suspend fun delete(id: Long): DomainResult<Unit>
    suspend fun move(cardId: Long, targetListId: Long, newPosition: Int): DomainResult<Card>
}
