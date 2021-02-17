package gb.smartchat.ui.chat.state_machine

import gb.smartchat.entity.Message

sealed class SideEffect {
    data class SendMessage(val message: Message) : SideEffect()
    data class TypingTimer(val senderId: String) : SideEffect()
    data class EditMessage(val message: Message, val newText: String) : SideEffect()
    data class DeleteMessage(val message: Message) : SideEffect()
    data class SetInputText(val text: String) : SideEffect()
    data class LoadPage(val fromMessageId: Long?, val forward: Boolean) : SideEffect()
    data class PageErrorEvent(val throwable: Throwable) : SideEffect()

    data class LoadSpecificPart(val fromMessageId: Long): SideEffect()
    data class InstaScrollTo(val position: Int): SideEffect()
}