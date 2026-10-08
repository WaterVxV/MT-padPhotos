package io.github.watervxv.mtpadphotos.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 动态 baseUrl 拦截器：每个请求发出前，把 URL 的 scheme/host/port 改写为
 * 当前保存的服务器地址。解决 Retrofit 在首次启动（尚未填写服务器地址）时
 * 把 baseUrl 永久锁死为 http://localhost/ 的问题。
 *
 * 注意：服务器地址只支持 scheme://host:port 形式，不支持带路径前缀。
 */
class DynamicBaseUrlInterceptor(private val serverUrlProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = serverUrlProvider()?.trimEnd('/') ?: return chain.proceed(request)
        val baseUrl = base.toHttpUrlOrNull() ?: return chain.proceed(request)

        val newUrl = request.url.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
