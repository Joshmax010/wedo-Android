package com.example.nutrition.domain.model

/**
 * 操作结果包装 —— 统一写操作的结果反馈
 *
 * Repository 写操作返回 Resource<Unit>，成功携带数据、失败携带用户可读的错误信息，
 * 替代原来只返回 Boolean 导致调用方需要自行猜测失败原因的问题
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    /** 成功时返回数据，失败时返回 null */
    fun getOrNull(): T? = (this as? Success)?.data

    companion object {
        fun success(): Resource<Unit> = Success(Unit)
    }
}
