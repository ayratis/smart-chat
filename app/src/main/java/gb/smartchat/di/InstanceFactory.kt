package gb.smartchat.di

import com.google.gson.Gson
import gb.smartchat.data.socket.SocketApi
import gb.smartchat.data.socket.SocketApiImpl
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URI
import java.net.URISyntaxException

object InstanceFactory {

    fun createSocket(url: String, userId: String): Socket {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor {
                val request = it.request()
                val newRequest =
                    request.newBuilder().addHeader("smart-user-id", userId)
                        .build()
                it.proceed(newRequest)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
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

    fun createSocketApi(): SocketApi {
        val socket: Socket = createSocket(
            url = "http://91.201.41.157:8000",
            userId = "77f21ecc-0d4a-4f85-9173-55acf327f007"
        )
        val gson = Gson()
        return SocketApiImpl(socket, gson)
    }
}