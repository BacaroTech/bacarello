package com.trellodelbacaro.application.service

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Card
import com.trellodelbacaro.domain.repository.CardRepository

class CardService(private val cardRepo: CardRepository) {
    suspend fun findById(id: Long): DomainResult<Card> = cardRepo.findById(id)
    suspend fun findByList(listId: Long): DomainResult<List<Card>> = cardRepo.findByList(listId)
    suspend fun create(listId: Long, card: Card): DomainResult<Card> = cardRepo.create(listId, card)
    suspend fun update(card: Card): DomainResult<Card> = cardRepo.update(card)
    suspend fun delete(id: Long): DomainResult<Unit> = cardRepo.delete(id)
    suspend fun move(cardId: Long, targetListId: Long, newPosition: Int): DomainResult<Card> =
        cardRepo.move(cardId, targetListId, newPosition)
}
