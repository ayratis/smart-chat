package gb.smartchat.data.socket

import android.util.Log
import com.google.gson.Gson
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.BuildConfig
import gb.smartchat.entity.Message
import gb.smartchat.entity.request.*
import gb.smartchat.utils.emitSingle
import gb.smartchat.utils.setSystemListeners
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

    private val socketEventsRelay = PublishRelay.create<SocketEvent>()

    init {
        if (BuildConfig.DEBUG) {
            socket.setSystemListeners {
                log(it)
            }
        }
        ServerEvent.values().forEach {
            setServerEventListener(it)
        }
    }

    private fun setServerEventListener(serverEvent: ServerEvent) {
        socket.on(serverEvent.eventName) { args ->
            try {
                val response = args[0] as JSONObject
                log("${serverEvent.eventName}: $response")
                val socketEvent = when (serverEvent) {
                    ServerEvent.MESSAGE_NEW -> {
                        val messageRaw = response.getJSONObject("new_message").toString()
                        val message = gson.fromJson(messageRaw, Message::class.java)
                        SocketEvent.MessageNew(message)
                    }
                    ServerEvent.MESSAGE_CHANGE -> {
                        val messageRaw = response.getJSONObject("new_message").toString()
                        val message = gson.fromJson(messageRaw, Message::class.java)
                        SocketEvent.MessageChange(message)
                    }
                    ServerEvent.TYPING -> {
                        val senderId = response.getString("sender_id")
                        SocketEvent.Typing(senderId)
                    }
                    ServerEvent.READ -> {
                        val messageIdsRaw = response.getJSONArray("messages_id").toString()
                        val messageIds = gson.fromJson(messageIdsRaw, Array<Long>::class.java)
                        SocketEvent.MessageRead(messageIds.toList())
                    }
                    else -> null
                }
                socketEvent?.let { socketEventsRelay.accept(it) }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
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

    override fun observeEvents(): Observable<SocketEvent> {
        return socketEventsRelay.hide()
    }

    override fun sendMessage(messageCreateRequest: MessageCreateRequest): Single<Boolean> {
        return sendEvent("usr:msg:edit", messageCreateRequest)
            .map { response ->
                response.getJSONObject("result").getBoolean("success")
            }
    }

    override fun editMessage(messageEditRequest: MessageEditRequest): Single<Boolean> {
        return sendEvent("usr:msg:edit", messageEditRequest)
            .map { response ->
                response.getJSONObject("result").getBoolean("success")
            }
    }

    override fun readMessage(messageReadRequest: MessageReadRequest): Single<Int> {
        return sendEvent("usr:msg:read", messageReadRequest)
            .map { response ->
                response.getJSONObject("result").getInt("read_message_count")
            }
    }

    override fun deleteMessage(messageDeleteRequest: MessageDeleteRequest): Single<Int> {
        return sendEvent("usr:msg:delete", messageDeleteRequest)
            .map { response ->
                response.getJSONObject("result").getInt("read_message_count")
            }
    }

    override fun sendTyping(typingRequest: TypingRequest): Single<Any> {
        return sendEvent("usr:typing:typing", typingRequest)
            .map { Any() }
    }

    private fun <R> sendEvent(method: String, body: R): Single<JSONObject> {
        val requestBody = JSONObject(gson.toJson(body))
        return socket.emitSingle(method, requestBody)
            .doOnSuccess { logAck(method, requestBody, it) }
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