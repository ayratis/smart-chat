package gb.smartchat.ui.chat.state_machine

import android.net.Uri
import gb.smartchat.entity.Message
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.SingleEvent

data class State(
    val chatItems: List<ChatItem> = emptyList(),
    val typingSenderIds: List<String> = emptyList(),
    val editingMessage: Message? = null,
    val currentText: String = "",
    val attachedPhoto: Uri? = null,
    val attachedFile: Uri? = null,
    val quotingMessage: Message? = null,
    val pagingState: PagingState = PagingState.EMPTY,
    val isOnline: Boolean = true,
    val chatEnabled: Boolean = true,
    val withScrollTo: SingleEvent<Int>? = null,
    val fullDataUp: Boolean = false,
    val fullDataDown: Boolean = false
)

enum class PagingState {
    EMPTY,
    EMPTY_PROGRESS,
    EMPTY_ERROR,
    DATA,
    REFRESH,
    NEW_PAGE_UP_PROGRESS,
    NEW_PAGE_DOWN_PROGRESS,
    NEW_PAGE_UP_DOWN_PROGRESS,
}