package gb.smartchat.ui.chat.state_machine

import gb.smartchat.entity.Message


sealed class Action {
    data class ClientSendMessage(val text: String) : Action()
    data class ServerMessageSent(val message: Message): Action()
    data class ServerMessageSendError(val message: Message): Action()
    data class ServerNewMessage(val message: Message): Action()
}