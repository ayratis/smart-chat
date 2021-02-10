package gb.smartchat.data.socket

import gb.smartchat.entity.request.MessageCreateRequest
import gb.smartchat.entity.request.MessageEditRequest
import gb.smartchat.entity.request.MessageReadRequest
import io.reactivex.Observable
import io.reactivex.Single

interface SocketApi {
    fun isConnected(): Boolean
    fun connect()
    fun disconnect()
    fun observeEvents(): Observable<SocketEvent>
    fun sendMessage(messageCreateRequest: MessageCreateRequest): Single<Boolean>
    fun editMessage(messageEditRequest: MessageEditRequest): Single<Boolean>
    fun readMessage(messageReadRequest: MessageReadRequest): Single<Int>
}