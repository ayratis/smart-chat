package gb.smartchat.library.data.socket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.BuildConfig
import gb.smartchat.library.entity.*
import gb.smartchat.library.entity.request.*
import gb.smartchat.library.entity.response.AddRecipientsResponse
import gb.smartchat.library.entity.response.DeleteRecipientsResponse
import io.reactivex.Observable
import io.reactivex.Single
import io.socket.client.Ack
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
        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "connect")
            socketEventsRelay.accept(SocketEvent.Connected)
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "disconnect")
            socketEventsRelay.accept(SocketEvent.Disconnected)
        }
        if (BuildConfig.DEBUG) {
            socket.on(Socket.EVENT_CONNECT_ERROR) {
                val msg = "connect error: ${(it[0] as? Exception)?.message}"
                Log.d(TAG, "connect error: $msg")
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
                        val changedMessage = gson.fromJson(messageRaw, ChangedMessage::class.java)
                        SocketEvent.MessageChange(changedMessage)
                    }
                    ServerEvent.TYPING -> {
                        val typing = gson.fromJson(response.toString(), Typing::class.java)
                        SocketEvent.Typing(typing)
                    }
                    ServerEvent.READ -> {
                        val messageRead =
                            gson.fromJson(response.toString(), MessageRead::class.java)
                        SocketEvent.MessageRead(messageRead)
                    }
                    ServerEvent.MESSAGE_DELETE -> {
                        val messageIdsRaw =
                            response.getJSONArray("deleted_messages").toString()
                        val typeToken = object : TypeToken<List<Message>>() {}.type
                        val messages: List<Message> = gson.fromJson(messageIdsRaw, typeToken)
                        SocketEvent.MessagesDeleted(messages)
                    }
                    ServerEvent.ADD_RECIPIENTS -> {
                        val result =
                            gson.fromJson(response.toString(), AddRecipientsResponse::class.java)
                        SocketEvent.AddRecipients(result)
                    }
                    ServerEvent.DELETE_RECIPIENTS -> {
                        val result =
                            gson.fromJson(response.toString(), DeleteRecipientsResponse::class.java)
                        SocketEvent.DeleteRecipients(result)
                    }

                    ServerEvent.USERNAME_MISSING -> null
                    ServerEvent.USER_MISSING -> SocketEvent.UserMissing
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
                        is SocketEvent.UserMissing -> true
                        is SocketEvent.MessageChange -> socketEvent.changedMessage.chatId == chatId
                        is SocketEvent.MessageNew -> socketEvent.message.chatId == chatId
                        is SocketEvent.MessageRead -> socketEvent.messageRead.chatId == chatId
                        is SocketEvent.Typing -> socketEvent.typing.chatId == chatId
                        is SocketEvent.MessagesDeleted ->
                            socketEvent.messages.firstOrNull()?.chatId == chatId
                        is SocketEvent.AddRecipients ->
                            socketEvent.addRecipientsResponse.chatId == chatId
                        is SocketEvent.DeleteRecipients ->
                            socketEvent.deleteRecipientsResponse.chatId == chatId
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

    private fun Socket.emitSingle(method: String, bodyJson: JSONObject): Single<JSONObject> =
        Single.create { emitter ->
            try {
                val ack = Ack { args ->
                    val response = args[0] as JSONObject
                    if (!emitter.isDisposed) {
                        emitter.onSuccess(response)
                    }
                }
                this.emit(
                    method,
                    bodyJson,
                    ack
                )
            } catch (e: Throwable) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                } else {
                    return@create
                }
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
