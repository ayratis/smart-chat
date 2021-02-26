package gb.smartchat.utils

import com.google.gson.Gson
import io.reactivex.Single
import io.socket.client.Ack
import io.socket.client.Socket
import org.json.JSONObject

fun Socket.setSystemListeners(withPingPong: Boolean = false, log: (msg: String) -> Unit) {
    val socket = this
    socket.on(Socket.EVENT_CONNECT) {
        val msg = "connect"
        log(msg)
    }
    socket.on(Socket.EVENT_CONNECTING) {
        val msg = "connecting"
        log(msg)
    }
    socket.on(Socket.EVENT_DISCONNECT) {
        val msg = "disconnect"
        log(msg)
    }
    socket.on(Socket.EVENT_ERROR) {
        val msg = "event error: ${(it.getOrNull(0) as? Exception)?.message}"
        log(msg)
    }
    socket.on(Socket.EVENT_MESSAGE) {
        val msg = "message: ${it.getOrNull(0) as? String}"
        log(msg)
    }
    socket.on(Socket.EVENT_CONNECT_ERROR) {
        val msg = "connect error: ${(it[0] as? Exception)?.message}"
        log(msg)
    }
    socket.on(Socket.EVENT_CONNECT_TIMEOUT) {
        val msg = "connect timeout"
        log(msg)
    }
    socket.on(Socket.EVENT_RECONNECT) {
        val msg = "reconnect"
        log(msg)
    }
    socket.on(Socket.EVENT_RECONNECT_ERROR) {
        val msg = "reconnect error: ${(it[0] as? Exception)?.message}"
        log(msg)
    }
    socket.on(Socket.EVENT_RECONNECT_FAILED) {
        val msg = "reconnect failed"
        log(msg)
    }
    socket.on(Socket.EVENT_RECONNECT_ATTEMPT) {
        val msg = "reconnect attempt"
        log(msg)
    }
    socket.on(Socket.EVENT_RECONNECTING) {
        val msg = "reconnecting"
        log(msg)
    }
    if (withPingPong) {
        socket.on(Socket.EVENT_PING) {
            val msg = "ping"
            log(msg)
        }
        socket.on(Socket.EVENT_PONG) {
            val msg = "pong"
            log(msg)
        }
    }
}


inline fun <reified T> Single<JSONObject>.parseResult(gson: Gson): Single<T> =
    this.map { jsonObject ->
        val jsonString = jsonObject.toString()
        val result = gson.fromJson(jsonString, T::class.java)
        result
    }

//inline fun <reified T> Socket.emmit(method: String, bodyJson: JSONObject, gson: Gson): Single<T> =
//    this.emmit(method, bodyJson).parseResult(gson)

fun Socket.emitSingle(method: String, bodyJson: JSONObject): Single<JSONObject> =
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
