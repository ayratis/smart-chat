package gb.smartchat.ui.chat.state_machine

import gb.smartchat.entity.Message


sealed class Action {

    data class ClientSendMessage(val text: String) : Action()
    data class ClientEditMessageRequest(val message: Message): Action()
    data class ClientEditMessageConfirm(val message: Message, val newText: String): Action()

    data class ServerMessageNew(val message: Message): Action()
    data class ServerMessageChange(val message: Message): Action()
    data class ServerTyping(val senderId: String): Action()
    data class ServerMessageRead(val messageIds: List<Long>): Action()

    data class ServerMessageSendSuccess(val message: Message): Action()
    data class ServerMessageSendError(val message: Message): Action()
    data class ServerMessageEditSuccess(val message: Message): Action()
    data class ServerMessageEditError(val message: Message): Action()

    data class InternalTypingTimeIsUp(val senderId: String): Action()
}