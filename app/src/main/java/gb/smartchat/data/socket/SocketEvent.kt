package gb.smartchat.data.socket

import gb.smartchat.entity.Message

sealed class SocketEvent {
    data class MessageNew(val message: Message) : SocketEvent()
    data class MessageChange(val message: Message) : SocketEvent()
    data class Typing(val senderId: String) : SocketEvent()
}

enum class ServerEvent(val eventName: String) {
    MESSAGE_NEW("srv:msg:new"),
    MESSAGE_CHANGE("srv:msg:change"),
    TYPING("srv:typing"),
    READ("srv:msg:read"),
    MESSAGE_DELETE("srv:msg:delete"),
    USERNAME_MISSING("srv:username:missing"),
    USER_MISSING("srv:user:missing")
}