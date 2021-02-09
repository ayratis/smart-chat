package gb.smartchat.ui.chat.state_machine

import gb.smartchat.entity.Message

sealed class SideEffect {
    data class SendMessage(val message: Message) : SideEffect()
    data class TypingTimer(val senderId: String) : SideEffect()
}