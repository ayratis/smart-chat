package gb.smartchat.data.socket

import gb.smartchat.entity.request.MessageCreateRequest
import gb.smartchat.entity.request.MessageEditRequest
import io.reactivex.Observable
import io.reactivex.Single

interface SocketApi {
    fun isConnected(): Boolean
    fun connect()
    fun disconnect()
    fun observeEvents(): Observable<SocketEvent>
    fun sendMessage(message: MessageCreateRequest): Single<Boolean>
    fun editMessage(messageEditRequest: MessageEditRequest): Single<Boolean>
}