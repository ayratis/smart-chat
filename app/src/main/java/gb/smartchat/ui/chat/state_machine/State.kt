package gb.smartchat.ui.chat.state_machine

import gb.smartchat.entity.Message
import gb.smartchat.ui.chat.ChatItem

data class State(
    val chatItems: List<ChatItem> = emptyList(),
    val typingSenderIds: List<String> = emptyList(),
    val editingMessage: Message? = null
)