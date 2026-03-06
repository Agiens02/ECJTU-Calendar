package com.lonx.ecjtu.calendar.data.datasource.remote

import com.lonx.ecjtu.calendar.util.Logger
import com.lonx.ecjtu.calendar.util.Logger.Tags
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import rxhttp.toAwait
import rxhttp.wrapper.param.RxHttp
import java.io.IOException


class JwxtDataSourceImpl : JwxtDataSource {

    private companion object {
        const val REQUEST_TIMEOUT_MS = 10_000L
        const val MAX_RETRY_COUNT = 2
        const val RETRY_DELAY_MS = 300L
    }

    override suspend fun fetchHtml(url: String, params: Map<String, Any>?): Result<String> {
        Logger.logRequestStart(Tags.JWXT_API, url, params?.size ?: 0)

        var attempt = 0
        while (attempt <= MAX_RETRY_COUNT) {
            try {
                val response = withTimeout(REQUEST_TIMEOUT_MS) {
                    val request = RxHttp.get(url)
                    params?.forEach { (key, value) ->
                        request.add(key, value)
                    }
                    request.toAwait<String>().await()
                }

                Logger.logRequestSuccess(Tags.JWXT_API, response.length)
                return Result.success(response)
            } catch (e: Exception) {
                val canRetry = attempt < MAX_RETRY_COUNT && e.isRetriable()
                if (!canRetry) {
                    val errorMessage = when (e) {
                        is TimeoutCancellationException -> "请求超时"
                        else -> e.message ?: "未知错误"
                    }
                    Logger.logRequestError(Tags.JWXT_API, errorMessage, e)
                    return Result.failure(e)
                }

                Logger.w(
                    Tags.JWXT_API,
                    "请求失败，准备重试: ${attempt + 1}/$MAX_RETRY_COUNT, error=${e.message}"
                )
                delay(RETRY_DELAY_MS)
                attempt++
            }
        }

        return Result.failure(IllegalStateException("请求状态异常"))
    }

    private fun Exception.isRetriable(): Boolean {
        return this is TimeoutCancellationException || this is IOException
    }
}