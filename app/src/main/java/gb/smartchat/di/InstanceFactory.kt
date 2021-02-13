package gb.smartchat.di

import com.google.gson.Gson
import gb.smartchat.data.http.HttpApi
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.net.URISyntaxException

object InstanceFactory {

    fun createHttpClient(userId: String): OkHttpClient {
        return with(OkHttpClient.Builder()) {
            addInterceptor {
                val request = it.request()
                val newRequest =
                    request.newBuilder().addHeader("smart-user-id", userId)
                        .build()
                it.proceed(newRequest)
            }
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            build()
        }
    }

    fun createSocket(url: String,  okHttpClient: OkHttpClient): Socket {
        val options = IO.Options().apply {
            callFactory = okHttpClient
            webSocketFactory = okHttpClient
        }
        return try {
            val manager = Manager(URI(url), options)
            val socket = manager.socket("/chat_v1").apply {
                io().timeout(-1)
            }
            socket
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    fun createHttpApi(okHttpClient: OkHttpClient, gson: Gson, url: String): HttpApi {
        val retrofit = with(Retrofit.Builder()) {
            addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            addConverterFactory(GsonConverterFactory.create(gson))
            client(okHttpClient)
            baseUrl(url)
            build()
        }
        return retrofit.create(HttpApi::class.java)
    }
}