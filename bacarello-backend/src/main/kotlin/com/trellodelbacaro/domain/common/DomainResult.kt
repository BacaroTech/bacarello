package com.trellodelbacaro.domain.common

sealed class DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>()

    sealed class Failure : DomainResult<Nothing>() {
        data class NotFound(val message: String) : Failure()
        data class ValidationError(val errors: List<String>) : Failure()
        data class Unauthorized(val message: String) : Failure()
        data class Forbidden(val message: String) : Failure()
        data class InternalError(val exception: Throwable) : Failure()
    }

    fun <R> map(transform: (T) -> R): DomainResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}
