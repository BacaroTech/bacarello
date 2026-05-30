package com.trellodelbacaro.infrastructure.persistence

import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.User
import com.trellodelbacaro.domain.repository.UserRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedUserRepository(private val database: org.jetbrains.exposed.sql.Database) : UserRepository {

    override suspend fun findByEmail(email: String): DomainResult<User> = runCatching {
        transaction(database) {
            Users.selectAll().where { Users.email eq email }.singleOrNull()
                ?.let { DomainResult.Success(it.toUser()) }
                ?: DomainResult.Failure.NotFound("User with email $email not found")
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    override suspend fun create(user: User): DomainResult<User> = runCatching {
        transaction(database) {
            val now = Clock.System.now()
            val newId = Users.insert {
                it[email] = user.email
                it[displayName] = user.displayName
                it[passwordHash] = user.passwordHash
                it[createdAt] = now
            } get Users.id
            DomainResult.Success(user.copy(id = newId, createdAt = now))
        }
    }.getOrElse { DomainResult.Failure.InternalError(it) }

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        email = this[Users.email],
        displayName = this[Users.displayName],
        passwordHash = this[Users.passwordHash],
        createdAt = this[Users.createdAt]
    )
}
