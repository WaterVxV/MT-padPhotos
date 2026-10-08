package io.github.watervxv.mtpadphotos.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val apiKeyProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val apiKey = apiKeyProvider() ?: return chain.proceed(request)

        val newRequest = request.newBuilder()
            .header("x-api-key", apiKey)
            .build()
        return chain.proceed(newRequest)
    }
}
