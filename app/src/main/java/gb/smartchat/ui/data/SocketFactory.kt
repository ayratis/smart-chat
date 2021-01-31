package gb.smartchat.ui.data

import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URI
import java.net.URISyntaxException

object SocketFactory {

    fun createWebSocketOptions(userId: String): IO.Options {

        return IO.Options().apply {
            val okHttpClient = with(OkHttpClient.Builder()) {
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
            callFactory = okHttpClient
            webSocketFactory = okHttpClient
        }
    }

    fun createSocket(url: String, userId: String): Socket {
        val options = createWebSocketOptions(userId)
        return try {
            val manager = Manager(URI(url))
            val socket = manager.socket("/chat_v1", options).apply {
                io().timeout(-1)
            }
            socket
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

}