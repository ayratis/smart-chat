package gb.smartchat.data.socket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.BuildConfig
import gb.smartchat.entity.Message
import gb.smartchat.entity.ReadInfo
import gb.smartchat.entity.Typing
import gb.smartchat.entity.request.*
import gb.smartchat.utils.emitSingle
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
//        if (BuildConfig.DEBUG) {
//            socket.setSystemListeners {
//                log(it)
//            }
//        }
        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "connect")
            socketEventsRelay.accept(SocketEvent.Connected)
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "disconnect")
            socketEventsRelay.accept(SocketEvent.Disconnected)
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
                        val typing = gson.fromJson(response.toString(), Typing::class.java)
                        SocketEvent.Typing(typing)
                    }
                    ServerEvent.READ -> {
                        val messageIdsRaw = response.getJSONArray("message_ids").toString()
                        val messageIds = gson.fromJson(messageIdsRaw, Array<Long>::class.java)
                        SocketEvent.MessageRead(messageIds.toList())
                    }
                    ServerEvent.MESSAGE_DELETE -> {
                        val messageIdsRaw =
                            response.getJSONArray("deleted_messages").toString()
                        val typeToken = object: TypeToken<List<Message>>(){}.type
                        val messages: List<Message> = gson.fromJson(messageIdsRaw, typeToken)
                        SocketEvent.MessagesDeleted(messages)
                    }
                    ServerEvent.USERNAME_MISSING -> null
                    ServerEvent.USER_MISSING -> null
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

    override fun observeEvents(chatId: Long?): Observable<SocketEvent> {
        return if (chatId == null) return socketEventsRelay.hide()
        else {
            val observable = socketEventsRelay.hide()
                .filter { socketEvent ->
                    when (socketEvent) {
                        is SocketEvent.Connected -> true
                        is SocketEvent.Disconnected -> true
                        is SocketEvent.MessageChange -> socketEvent.message.chatId == chatId
                        is SocketEvent.MessageNew -> socketEvent.message.chatId == chatId
                        is SocketEvent.MessageRead -> true
                        is SocketEvent.Typing -> socketEvent.typing.chatId == chatId
                        is SocketEvent.MessagesDeleted ->
                            socketEvent.messages.firstOrNull()?.chatId == chatId
                    }
                }
            if (isConnected()) observable.startWith(SocketEvent.Connected)
            else observable
        }

    }

    override fun sendMessage(messageCreateRequest: MessageCreateRequest): Single<Boolean> {
        return sendEvent("usr:msg:create", messageCreateRequest)
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

    override fun deleteMessage(messageDeleteRequest: MessageDeleteRequest): Single<Boolean> {
        return sendEvent("usr:msg:delete", messageDeleteRequest)
            .map { response ->
                response.getJSONObject("result").getBoolean("success")
            }
    }

    override fun sendTyping(typingRequest: TypingRequest): Single<Any> {
        return sendEvent("usr:typing:typing", typingRequest)
            .map { Any() }
    }

    override fun getReadInfo(readInfoRequest: ReadInfoRequest): Single<ReadInfo> {
        return sendEvent("usr:chat:read_info", readInfoRequest)
            .map { response ->
                val raw = response.getJSONObject("result").toString()
                gson.fromJson(raw, ReadInfo::class.java)
            }
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
