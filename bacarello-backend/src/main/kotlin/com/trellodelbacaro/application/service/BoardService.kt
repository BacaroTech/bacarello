package com.trellodelbacaro.application.service

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.Board
import com.trellodelbacaro.domain.repository.BoardRepository

class BoardService(private val boardRepo: BoardRepository) {
    suspend fun findAll(): DomainResult<List<Board>> = boardRepo.findAll()
    suspend fun findById(id: Long): DomainResult<Board> = boardRepo.findById(id)
    suspend fun create(board: Board): DomainResult<Board> = boardRepo.create(board)
    suspend fun update(board: Board): DomainResult<Board> = boardRepo.update(board)
    suspend fun delete(id: Long): DomainResult<Unit> = boardRepo.delete(id)
}
