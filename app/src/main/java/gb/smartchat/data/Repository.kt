package gb.smartchat.data

import gb.smartchat.data.socket.SocketApi
import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageCreateRequest
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.ConcurrentLinkedQueue

class Repository(
    private val socketApi: SocketApi
) {
    private var outboxMessageId: Int = 1
    private val outboxMessages = ConcurrentLinkedQueue<MessageCreateRequest>()


    fun connect() {
        if (!socketApi.isConnected()) {
            socketApi.connect()
        }
    }

    fun disconnect() {
        socketApi.disconnect()
    }

    fun sendMessage(text: String): Single<Boolean> {
       return socketApi
            .sendMessage(
                MessageCreateRequest(
                    text = text,
                    senderId = "77f21ecc-0d4a-4f85-9173-55acf327f007",
                    chatId = 1,
                    clientId = "1",
                )
            )
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun sendMessage(message: Message): Single<Boolean> {
        return socketApi
            .sendMessage(
                MessageCreateRequest(
                    text = message.text,
                    senderId = message.senderId!!,
                    chatId = 1,
                    clientId = message.clientId!!,
                )
            )
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun observeNewMessages(): Observable<Message> = socketApi
        .observeNewMessages()
        .observeOn(AndroidSchedulers.mainThread())

}