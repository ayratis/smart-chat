package gb.smartchat.ui.chat.state_machine

import gb.smartchat.entity.Message

sealed class SideEffect {
    data class SendMessage(val message: Message) : SideEffect()
    data class TypingTimer(val senderId: String) : SideEffect()
    data class EditMessage(val message: Message, val newText: String) : SideEffect()
    data class DeleteMessage(val message: Message) : SideEffect()
    data class SetInputText(val text: String) : SideEffect()
}