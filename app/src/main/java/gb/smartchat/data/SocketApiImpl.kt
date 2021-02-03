package gb.smartchat.data

import android.util.Log
import com.google.gson.Gson
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.BuildConfig
import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageCreateRequest
import io.reactivex.Observable
import io.reactivex.Single
import io.socket.client.Socket
import org.json.JSONObject

class SocketApiImpl(
    private val socket: Socket,
    private val gson: Gson
) : SocketApi {

    companion object {
        const val TAG = "Socket"
    }

    private val newMessageRelay = PublishRelay.create<Message>()

    init {
        if (BuildConfig.DEBUG) {
            socket.setSystemListeners { log(it) }
        }
        socket.on("srv:msg:new") {
            val msg = "srv:msg:new: ${it[0] as? JSONObject}"
            log(msg)
        }
//        setServerListeners()
    }

    override fun isConnected(): Boolean {
        return socket.connected()
    }

    override fun connect() {
        socket.connect()
    }

    override fun disconnect() {
        socket.disconnect()
    }

    override fun observeNewMessages(): Observable<Message> =
        newMessageRelay.hide()

    override fun sendMessage(message: MessageCreateRequest): Single<Boolean> {
        val method = "usr:msg:create"
        val requestBody = message.toJSONObject()
        return socket.emitSingle(method, requestBody)
            .map { response ->
                logAck(method, requestBody, response)
                try {
                    response.getJSONObject("result").getBoolean("success")
                } catch (e: Throwable) {
                    false
                }
            }
    }

    private fun setServerListeners() {
        socket.on("srv:msg:new") {
            log("srv:msg:new")
            try {
                val response = it[0] as JSONObject
                log("srv:msg:new: $response")
                val message = gson.fromJson(response.toString(), Message::class.java)
                newMessageRelay.accept(message)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        socket.on("srv:msg:change") {
            val msg = "srv:msg:change: ${it[0] as? JSONObject}"
            log(msg)
        }
        socket.on("srv:typing") {
            val msg = "srv:typing: ${it[0] as? JSONObject}"
            log(msg)
        }
        socket.on("srv:msg:read") {
            val msg = "srv:msg:read: ${it[0] as? JSONObject}"
            log(msg)
        }
        socket.on("srv:msg:delete") {
            val msg = "srv:msg:delete: ${it[0] as? JSONObject}"
            log(msg)
        }
        socket.on("srv:username:missing") {
            val msg = "srv:username:missing: ${it[0] as? JSONObject}"
            log(msg)
        }
        socket.on("srv:user:missing") {
            val msg = "srv:user:missing: ${it[0] as? JSONObject}"
            log(msg)
        }
    }

    private fun logAck(method: String, requestBody: JSONObject, responseBody: JSONObject) {
        val msg = "Ack callback\n" +
                "requestMethod: $method\n" +
                "requestBody: $requestBody\n" +
                "response: $responseBody"
        log(msg)
    }

    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, msg)
        }
    }

}