package com.trellodelbacaro.application.service

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.BoardList
import com.trellodelbacaro.domain.repository.ListRepository

class ListService(private val listRepo: ListRepository) {
    suspend fun findByBoard(boardId: Long): DomainResult<List<BoardList>> = listRepo.findByBoard(boardId)
    suspend fun create(boardId: Long, name: String, position: Int): DomainResult<BoardList> =
        listRepo.create(BoardList(boardId = boardId, name = name, position = position))
}
