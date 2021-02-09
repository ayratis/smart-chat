package gb.smartchat.ui.chat.state_machine

import gb.smartchat.entity.Message


sealed class Action {

    data class ClientSendMessage(val text: String) : Action()

    data class ServerMessageSent(val message: Message): Action()
    data class ServerMessageSendError(val message: Message): Action()
    data class ServerMessageNew(val message: Message): Action()
    data class ServerMessageChange(val message: Message): Action()
    data class ServerTyping(val senderId: String): Action()
    data class ServerMessageRead(val messageIds: List<Long>): Action()

    data class InternalTypingTimeIsUp(val senderId: String): Action()
}