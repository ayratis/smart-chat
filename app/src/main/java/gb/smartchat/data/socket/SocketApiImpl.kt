package gb.smartchat.data.socket

import android.util.Log
import com.google.gson.Gson
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.BuildConfig
import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageCreateRequest
import gb.smartchat.entity.response.NewMessage
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
            log(ServerEvent.MESSAGE_NEW.eventName)
            try {
                val response = args[0] as JSONObject
                log("${ServerEvent.MESSAGE_NEW.eventName}: $response")
                val socketEvent = when (serverEvent) {
                    ServerEvent.MESSAGE_NEW -> {
                        val message =
                            gson.fromJson(response.toString(), NewMessage::class.java).message
                        SocketEvent.MessageNew(message)
                    }
                    ServerEvent.MESSAGE_CHANGE -> {
                        val message =
                            gson.fromJson(response.toString(), NewMessage::class.java).message
                        SocketEvent.MessageChange(message)
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

    override fun sendMessage(message: MessageCreateRequest): Single<Boolean> {
        val method = "usr:msg:create"
        val requestBody = JSONObject(gson.toJson(message))
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