package gb.smartchat.data

import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageCreateRequest
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers

class Repository(
    private val socketApi: SocketApi
) {

    fun connect() {
        if (!socketApi.isConnected()) {
            socketApi.connect()
        }
    }

    fun disconnect() {
        socketApi.disconnect()
    }

    fun sendMessage(text: String): Single<Boolean> = socketApi
        .sendMessage(
            MessageCreateRequest(
                text = text,
                senderId = "77f21ecc-0d4a-4f85-9173-55acf327f007",
                chatId = 1,
                clientId = "f3191891-ca47-4eb0-9fc8-02cd7d3cf3e5",
            )
        )
        .observeOn(AndroidSchedulers.mainThread())

    fun observeNewMessages(): Observable<Message> = socketApi
        .observeNewMessages()
        .observeOn(AndroidSchedulers.mainThread())

}