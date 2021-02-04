package gb.smartchat.data.socket

import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageCreateRequest
import io.reactivex.Observable
import io.reactivex.Single

interface SocketApi {
    fun isConnected(): Boolean
    fun connect()
    fun disconnect()
    fun observeNewMessages(): Observable<Message>
    fun sendMessage(message: MessageCreateRequest): Single<Boolean>
}