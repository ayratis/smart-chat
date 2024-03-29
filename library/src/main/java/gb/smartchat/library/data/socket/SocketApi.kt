package gb.smartchat.library.data.socket

import gb.smartchat.library.entity.ReadInfo
import gb.smartchat.library.entity.request.*
import io.reactivex.Observable
import io.reactivex.Single

interface SocketApi {
    fun isConnected(): Boolean
    fun connect()
    fun disconnect()
    fun observeEvents(chatId: Long? = null): Observable<SocketEvent>

    fun sendMessage(messageCreateRequest: MessageCreateRequest): Single<Boolean>
    fun editMessage(messageEditRequest: MessageEditRequest): Single<Boolean>
    fun readMessage(messageReadRequest: MessageReadRequest): Single<Int>
    fun deleteMessage(messageDeleteRequest: MessageDeleteRequest): Single<Boolean>
    fun sendTyping(typingRequest: TypingRequest): Single<Any>
    fun getReadInfo(readInfoRequest: ReadInfoRequest): Single<ReadInfo>
}
