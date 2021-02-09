package gb.smartchat.data.socket

import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageCreateRequest
import io.reactivex.Observable
import io.reactivex.Single

interface SocketApi {
    fun isConnected(): Boolean
    fun connect()
    fun disconnect()
    fun observeEvents(): Observable<SocketEvent>
    fun sendMessage(message: MessageCreateRequest): Single<Boolean>
}