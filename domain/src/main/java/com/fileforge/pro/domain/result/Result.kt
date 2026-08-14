package com.fileforge.pro.domain.result

/**
 * App-wide Result wrapper (Master Spec §69 — Error Handling).
 *
 * Lives in :domain so it can be used by domain interfaces WITHOUT pulling
 * any Android dependency. The :core:common module re-exports this type so
 * callers in :data / :engine / :feature can use the same type.
 *
 * Design rules:
 *  - [Ok] always carries a non-null value.
 *  - [Err] always carries a typed [FileError].
 *  - Never throw in domain code — convert exceptions at the repository boundary.
 */
sealed interface Result<out T> {
    data class Ok<T>(val value: T) : Result<T>
    data class Err(val error: FileError) : Result<Nothing>

    fun getOrNull(): T? = (this as? Ok)?.value
    fun errorOrNull(): FileError? = (this as? Err)?.error
    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Ok -> Result.Ok(transform(value))
    is Result.Err -> this
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Ok) action(value)
    return this
}

inline fun <T> Result<T>.onFailure(action: (FileError) -> Unit): Result<T> {
    if (this is Result.Err) action(error)
    return this
}

inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Ok(block())
} catch (e: Throwable) {
    Result.Err(FileError.fromException(e))
}
