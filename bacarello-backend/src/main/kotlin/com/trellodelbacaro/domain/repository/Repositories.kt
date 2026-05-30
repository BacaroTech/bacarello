package com.trellodelbacaro.domain.repository

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import com.trellodelbacaro.domain.model.Card
import com.trellodelbacaro.domain.model.User

interface UserRepository {
    suspend fun findByEmail(email: String): DomainResult<User>
    suspend fun create(user: User): DomainResult<User>
}

interface BoardRepository {
    suspend fun findAll(): DomainResult<List<Board>>
    suspend fun findById(id: Long): DomainResult<Board>
    suspend fun create(board: Board): DomainResult<Board>
    suspend fun update(board: Board): DomainResult<Board>
    suspend fun delete(id: Long): DomainResult<Unit>
}

interface CardRepository {
    suspend fun findById(id: Long): DomainResult<Card>
    suspend fun create(listId: Long, card: Card): DomainResult<Card>
    suspend fun update(card: Card): DomainResult<Card>
    suspend fun delete(id: Long): DomainResult<Unit>
    suspend fun move(cardId: Long, targetListId: Long, newPosition: Int): DomainResult<Card>
}
