package gb.smartchat.data.socket

import gb.smartchat.entity.Message

sealed class SocketEvent {
    data class MessageNew(val message: Message) : SocketEvent()
    data class MessageChange(val message: Message) : SocketEvent()
    data class Typing(val senderId: String) : SocketEvent()
    data class MessageRead(val messageIds: List<Long>) : SocketEvent()
    object Connected : SocketEvent()
    object Disconnected : SocketEvent()
}

enum class ServerEvent(val eventName: String) {
    TYPING("srv:typing"),
    MESSAGE_NEW("srv:msg:new"),
    READ("srv:msg:read"),
    MESSAGE_CHANGE("srv:msg:change"),
    MESSAGE_DELETE("srv:msg:delete"),

    USERNAME_MISSING("srv:username:missing"),
    USER_MISSING("srv:user:missing")
}