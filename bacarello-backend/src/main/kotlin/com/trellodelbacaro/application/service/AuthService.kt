package com.trellodelbacaro.application.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.trellodelbacaro.domain.common.DomainResult
import com.trellodelbacaro.domain.model.User
import com.trellodelbacaro.domain.repository.UserRepository
import com.trellodelbacaro.infrastructure.security.JwtProvider
import com.trellodelbacaro.infrastructure.security.JwtToken

class AuthService(
    private val userRepo: UserRepository,
    private val jwtProvider: JwtProvider
) {
    suspend fun register(email: String, displayName: String, password: String): DomainResult<User> {
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        return userRepo.create(User(email = email, displayName = displayName, passwordHash = hash))
    }

    suspend fun login(email: String, password: String): DomainResult<JwtToken> {
        return when (val result = userRepo.findByEmail(email)) {
            is DomainResult.Success -> {
                val user = result.value
                val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
                if (verified) DomainResult.Success(jwtProvider.generate(user.id!!, user.email))
                else DomainResult.Failure.Unauthorized("Invalid credentials")
            }
            is DomainResult.Failure -> result
        }
    }
}
