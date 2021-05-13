package gb.smartchat.data.socket

import gb.smartchat.entity.ChangedMessage
import gb.smartchat.entity.Message
import gb.smartchat.entity.response.AddRecipientsResponse
import gb.smartchat.entity.response.DeleteRecipientsResponse

sealed class SocketEvent {
    data class MessageNew(val message: Message) : SocketEvent()
    data class MessageChange(val changedMessage: ChangedMessage) : SocketEvent()
    data class Typing(val typing: gb.smartchat.entity.Typing) : SocketEvent()
    data class MessageRead(val messageRead: gb.smartchat.entity.MessageRead) : SocketEvent()
    data class MessagesDeleted(val messages: List<Message>) : SocketEvent()
    data class AddRecipients(val addRecipientsResponse: AddRecipientsResponse) : SocketEvent()
    data class DeleteRecipients(
        val deleteRecipientsResponse: DeleteRecipientsResponse
    ) : SocketEvent()

    object Connected : SocketEvent()
    object Disconnected : SocketEvent()
}

enum class ServerEvent(val eventName: String) {
    TYPING("srv:typing"),
    MESSAGE_NEW("srv:msg:new"),
    READ("srv:msg:read"),
    MESSAGE_CHANGE("srv:msg:change"),
    MESSAGE_DELETE("srv:msg:delete"),
    ADD_RECIPIENTS("srv:chat:add_recipients"),
    DELETE_RECIPIENTS("srv:chat:delete_recipients"),

    USERNAME_MISSING("srv:username:missing"),
    USER_MISSING("srv:user:missing")
}
