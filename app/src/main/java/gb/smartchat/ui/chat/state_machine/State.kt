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
    val isOnline: Boolean = false,
    val chatEnabled: Boolean = true,
    val withScrollTo: SingleEvent<Int>? = null,
    val fullDataUp: Boolean = false,
    val fullDataDown: Boolean = false,
    val unreadMessageCount: Int = 0,
    val lastMessageId: Long? = null,
    val atBottom: Boolean = true
) {
    companion object {
        const val UNREAD_OVER_MAX_COUNT = -1
        const val DEFAULT_PAGE_SIZE = 30
    }
}

enum class PagingState {
    EMPTY,
    EMPTY_PROGRESS,
    EMPTY_ERROR,
    DATA,
    NEW_PAGE_UP_PROGRESS,
    NEW_PAGE_DOWN_PROGRESS,
    NEW_PAGE_UP_DOWN_PROGRESS,
}