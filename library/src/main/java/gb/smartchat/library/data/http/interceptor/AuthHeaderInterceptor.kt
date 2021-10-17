package gb.smartchat.library.data.http.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(private val userId: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest =
            request.newBuilder().addHeader("smart-user-id", userId)
                .build()
        return chain.proceed(newRequest)
    }
}