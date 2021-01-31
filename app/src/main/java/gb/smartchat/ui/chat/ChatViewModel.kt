package gb.smartchat.ui.chat

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import gb.smartchat.entity.request.MessageCreateRequest
import gb.smartchat.ui.data.SocketFactory
import io.socket.client.Ack
import io.socket.client.Socket
import org.json.JSONObject

class ChatViewModel(
    private val socket: Socket = SocketFactory.createSocket(
        url = "http://91.201.41.157:8000",
        userId = "77f21ecc-0d4a-4f85-9173-55acf327f007"
    ),
    private val gson: Gson = Gson()
) : ViewModel() {

    companion object {
        private const val TAG = "Socket"
    }

    val logList = MutableLiveData(listOf(ChatItem(1, "`````````````")))

    init {

        socket.on(Socket.EVENT_CONNECT) {
            val msg = "connect"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_CONNECTING) {
            val msg = "connecting"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            val msg = "disconnect"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_ERROR) {
            val msg = "event error: ${(it.getOrNull(0) as? Exception)?.message}"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_MESSAGE) {
            val msg = "message: ${it.getOrNull(0) as? String}"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            val msg = "connect error: ${(it[0] as? Exception)?.message}"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_CONNECT_TIMEOUT) {
            val msg = "connect timeout"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_RECONNECT) {
            val msg = "reconnect"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_RECONNECT_ERROR) {
            val msg = "reconnect error: ${(it[0] as? Exception)?.message}"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_RECONNECT_FAILED) {
            val msg = "reconnect failed"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_RECONNECT_ATTEMPT) {
            val msg = "reconnect attempt"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_RECONNECTING) {
            val msg = "reconnecting"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_PING) {
            val msg = "ping"
            addMsgToList(msg)
        }
        socket.on(Socket.EVENT_PONG) {
            val msg = "pong"
            addMsgToList(msg)
        }
        //server custom
        socket.on("srv:msg:new") {
            val msg = "onNewMessage: $it"
            addMsgToList(msg)
        }
    }

    fun onStart() {
        if (!socket.connected()) {
            socket.connect()
        }
    }

    fun onSendClick(text: String) {
        val body =
            MessageCreateRequest(
                text = text,
                senderId = "77f21ecc-0d4a-4f85-9173-55acf327f007",
                chatId = 1,
                clientId = "f3191891-ca47-4eb0-9fc8-02cd7d3cf3e5",
            )
        val bodyJson = JSONObject(gson.toJson(body))
        socket.emit(
            "usr:msg:create",
            bodyJson,
            Ack { args ->
                val response = args.getOrNull(0) as? JSONObject
                val msg =
                    "Ack callback\nrequest: usr:msg:create\nrequestBody: $body\nresponse: $response"
                addMsgToList(msg)
            }
        )
    }

    private fun addMsgToList(msg: String) {
        Log.d(TAG, msg)
        val list = logList.value?.toMutableList() ?: mutableListOf()
        val lastItemId = list.lastOrNull()?.id ?: 1
        list += ChatItem(lastItemId + 1, msg)
        logList.postValue(list)
    }

    override fun onCleared() {
        socket.disconnect()
    }

}