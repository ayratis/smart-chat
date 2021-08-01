package gb.smartchat.library.ui.chat_list

import android.util.SparseArray
import androidx.core.util.forEach
import gb.smartchat.library.entity.*
import gb.smartchat.library.ui._global.BaseStore
import gb.smartchat.library.utils.composeWithMessage

object ChatListUDF {

    const val DEFAULT_PAGE_SIZE = 20
    private const val TAG = "ChatListStateMachine"

    data class State(
        val chatList: List<Chat> = emptyList(),
        val pagingState: PagingState = PagingState.EMPTY,
        val pageCount: Int = 0
    )

    enum class PagingState {
        EMPTY,
        EMPTY_PROGRESS,
        EMPTY_ERROR,
        DATA,
        REFRESH,
        NEW_PAGE_PROGRESS,
        FULL_DATA
    }

    sealed class Action {
        object Refresh : Action()
        object LoadMore : Action()
        data class NewPage(val pageNumber: Int, val items: List<Chat>) : Action()
        data class PageError(val error: Throwable) : Action()
        data class NewMessage(val message: Message) : Action()
        data class ReadMessage(val messageRead: MessageRead) : Action()
        data class ChangeMessage(val changedMessage: ChangedMessage) : Action()
        data class DeleteMessages(val deletedMessages: List<Message>) : Action()
        data class ChatUnreadMessageCountChanged(val chatId: Long, val unreadCount: Int) : Action()
        data class NewChatCreated(val chat: Chat) : Action()
        data class PinChat(val chat: Chat, val pin: Boolean) : Action()
        data class PinChatSuccess(val chat: Chat, val pin: Boolean) : Action()
        data class PinChatError(val error: Throwable, val chat: Chat, val pin: Boolean) : Action()
        data class ArchiveChat(val chat: Chat, val archive: Boolean) : Action()
        data class ArchiveChatSuccess(val chat: Chat, val archive: Boolean) : Action()
        data class ArchiveChatError(
            val error: Throwable,
            val chat: Chat,
            val archive: Boolean
        ) : Action()
        data class ChatUnarchived(val chat: Chat) : Action()
        data class AddRecipients(val chatId: Long, val newRecipients: List<Contact>) : Action()
        data class DeleteRecipients(val chatId: Long, val deletedUserIds: List<String>) : Action()
        data class LeaveChat(val chat: Chat) : Action()
        data class ExternalChatArchived(val chat: Chat) : Action()
    }

    sealed class SideEffect {
        data class LoadPage(val pageCount: Int) : SideEffect()
        data class ErrorEvent(val error: Throwable) : SideEffect()
        data class PinChat(val chat: Chat, val pin: Boolean) : SideEffect()
        data class ShowPinChatError(val error: Throwable, val pin: Boolean) : SideEffect()
        data class ArchiveChat(val chat: Chat, val archive: Boolean) : SideEffect()
        data class ShowArchiveChatError(val error: Throwable, val archive: Boolean) : SideEffect()
    }

    class Store(private val userId: String) : BaseStore<State, Action, SideEffect>(State()) {

        override fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
            when (action) {
                is Action.Refresh -> {
                    sideEffectListener(SideEffect.LoadPage(1))
                    val newPagingState = when (state.pagingState) {
                        PagingState.EMPTY,
                        PagingState.EMPTY_ERROR,
                        PagingState.EMPTY_PROGRESS -> PagingState.EMPTY_PROGRESS
                        PagingState.DATA,
                        PagingState.NEW_PAGE_PROGRESS,
                        PagingState.FULL_DATA,
                        PagingState.REFRESH -> PagingState.REFRESH
                    }
                    return state.copy(pagingState = newPagingState)
                }
                is Action.LoadMore -> {
                    return when (state.pagingState) {
                        PagingState.DATA -> {
                            sideEffectListener(SideEffect.LoadPage(state.pageCount + 1))
                            state.copy(pagingState = PagingState.NEW_PAGE_PROGRESS)
                        }
                        else -> state
                    }
                }
                is Action.NewPage -> {
                    return when (state.pagingState) {
                        PagingState.EMPTY_PROGRESS, PagingState.REFRESH -> {
                            if (action.items.isEmpty()) {
                                state.copy(
                                    chatList = emptyList(),
                                    pagingState = PagingState.EMPTY
                                )
                            } else {
                                state.copy(
                                    chatList = action.items,
                                    pagingState = PagingState.DATA,
                                    pageCount = 1
                                )
                            }
                        }
                        PagingState.NEW_PAGE_PROGRESS -> {
                            if (action.items.isEmpty()) {
                                state.copy(pagingState = PagingState.FULL_DATA)
                            } else {
                                state.copy(
                                    chatList = state.chatList + action.items,
                                    pagingState = PagingState.DATA,
                                    pageCount = state.pageCount + 1
                                )
                            }
                        }
                        else -> state
                    }
                }
                is Action.PageError -> {
                    return when (state.pagingState) {
                        PagingState.EMPTY_PROGRESS -> {
                            state.copy(pagingState = PagingState.EMPTY_ERROR)
                        }
                        PagingState.REFRESH -> {
                            sideEffectListener(SideEffect.ErrorEvent(action.error))
                            state.copy(pagingState = PagingState.DATA)
                        }
                        PagingState.NEW_PAGE_PROGRESS -> {
                            sideEffectListener(SideEffect.ErrorEvent(action.error))
                            state.copy(pagingState = PagingState.DATA)
                        }
                        else -> state
                    }
                }
                is Action.NewMessage -> {
                    val targetPosition =
                        state.chatList.indexOfFirst { it.id == action.message.chatId }
                    if (targetPosition >= 0) {
                        val newChatList = state.chatList.toMutableList().apply {
                            val oldChat = get(targetPosition)
                            var unreadMessageCount = oldChat.unreadMessagesCount ?: 0
                            if (!action.message.isOutgoing(userId)) unreadMessageCount++
                            val newChat = oldChat.copy(
                                lastMessage = action.message,
                                unreadMessagesCount = unreadMessageCount
                            )
                            set(targetPosition, newChat)
                            sort()
                        }
                        return state.copy(chatList = newChatList)
                    } else {
                        sideEffectListener(SideEffect.LoadPage(1))
                        val newPagingState = when (state.pagingState) {
                            PagingState.EMPTY,
                            PagingState.EMPTY_ERROR,
                            PagingState.EMPTY_PROGRESS -> PagingState.EMPTY_PROGRESS
                            PagingState.DATA,
                            PagingState.NEW_PAGE_PROGRESS,
                            PagingState.FULL_DATA,
                            PagingState.REFRESH -> PagingState.REFRESH
                        }
                        return state.copy(pagingState = newPagingState)
                    }
                }
                is Action.ReadMessage -> {
                    val targetPosition =
                        state.chatList.indexOfFirst { it.id == action.messageRead.chatId }
                    if (targetPosition >= 0) {
                        val newChatList = state.chatList.toMutableList().apply {
                            val newUsers = get(targetPosition).users.map { user ->
                                if (user.id == action.messageRead.senderId) {
                                    val lastReadMessageId =
                                        action.messageRead.messageIds.maxOf { it }
                                    user.copy(lastReadMessageId = lastReadMessageId)
                                } else {
                                    user
                                }
                            }
                            val chat = get(targetPosition).copy(users = newUsers)
                            set(targetPosition, chat)
                        }
                        return state.copy(chatList = newChatList)
                    } else {
                        return state
                    }
                }
                is Action.ChangeMessage -> {
                    val targetPosition =
                        state.chatList.indexOfFirst { it.id == action.changedMessage.chatId }
                    return if (targetPosition >= 0) {
                        val newChatList = state.chatList.toMutableList().apply {
                            val chat = get(targetPosition)
                            if (chat.lastMessage?.id == action.changedMessage.id) {
                                val newMessage =
                                    action.changedMessage.composeWithMessage(chat.lastMessage)
                                set(targetPosition, chat.copy(lastMessage = newMessage))
                            }
                        }
                        state.copy(chatList = newChatList)
                    } else {
                        state
                    }
                }
                is Action.DeleteMessages -> {
                    val targetAndMessages = SparseArray<Message>()
                    for (deletedMessage in action.deletedMessages) {
                        val targetPosition =
                            state.chatList.indexOfFirst { it.id == deletedMessage.chatId }
                        if (targetPosition >= 0) {
                            targetAndMessages.put(targetPosition, deletedMessage)
                        }
                    }
                    val newChatList = state.chatList.toMutableList().apply {
                        targetAndMessages.forEach { key, value ->
                            val newMessage = get(key).copy(lastMessage = value)
                            set(key, newMessage)
                        }
                    }
                    return state.copy(chatList = newChatList)
                }
                is Action.ChatUnreadMessageCountChanged -> {
                    val targetPosition = state.chatList.indexOfFirst { it.id == action.chatId }
                    return if (targetPosition >= 0) {
                        val newChatList = state.chatList.toMutableList().apply {
                            val chat =
                                get(targetPosition).copy(unreadMessagesCount = action.unreadCount)
                            set(targetPosition, chat)
                        }
                        state.copy(chatList = newChatList)
                    } else {
                        state
                    }
                }
                is Action.NewChatCreated -> {
                    val newChatList = state.chatList.toMutableList().apply {
                        add(0, action.chat)
                    }
                    sideEffectListener(SideEffect.LoadPage(1))
                    val newPagingState = when (state.pagingState) {
                        PagingState.EMPTY,
                        PagingState.EMPTY_ERROR,
                        PagingState.EMPTY_PROGRESS -> PagingState.EMPTY_PROGRESS
                        PagingState.DATA,
                        PagingState.NEW_PAGE_PROGRESS,
                        PagingState.FULL_DATA,
                        PagingState.REFRESH -> PagingState.REFRESH
                    }
                    return state.copy(chatList = newChatList, pagingState = newPagingState)
                }
                is Action.PinChat -> {
                    if (action.chat.isPinned == action.pin) return state
                    sideEffectListener.invoke(SideEffect.PinChat(action.chat, action.pin))
                    val newChat = action.chat.copy(isPinned = action.pin)
                    val newChatList = state.chatList.toMutableList().apply {
                        val pos = indexOf(action.chat)
                        set(pos, newChat)
                        sort()
                    }
                    return state.copy(chatList = newChatList)
                }
                is Action.PinChatSuccess -> {
                    return state
                }
                is Action.PinChatError -> {
                    sideEffectListener.invoke(SideEffect.ShowPinChatError(action.error, action.pin))
                    val newChatList = state.chatList.toMutableList().apply {
                        val pos = indexOfFirst { it.id == action.chat.id }
                        if (pos >= 0) {
                            set(pos, action.chat)
                            sort()
                        }
                    }
                    return state.copy(chatList = newChatList)
                }
                is Action.ArchiveChat -> {
                    sideEffectListener.invoke(SideEffect.ArchiveChat(action.chat, action.archive))
                    val newChatList = state.chatList.toMutableList().apply {
                        remove(action.chat)
                    }
                    return state.copy(chatList = newChatList)
                }
                is Action.ArchiveChatSuccess -> {
                    return state
                }
                is Action.ArchiveChatError -> {
                    sideEffectListener.invoke(
                        SideEffect.ShowArchiveChatError(
                            action.error,
                            action.archive
                        )
                    )
                    val newChatList = state.chatList.toMutableList().apply {
                        add(action.chat)
                        sort()
                    }
                    return state.copy(chatList = newChatList)
                }
                is Action.ChatUnarchived -> {
                    val newChatList = state.chatList.toMutableList().apply {
                        add(action.chat)
                        sort()
                    }
                    sideEffectListener(SideEffect.LoadPage(1))
                    val newPagingState = when (state.pagingState) {
                        PagingState.EMPTY,
                        PagingState.EMPTY_ERROR,
                        PagingState.EMPTY_PROGRESS -> PagingState.EMPTY_PROGRESS
                        PagingState.DATA,
                        PagingState.NEW_PAGE_PROGRESS,
                        PagingState.FULL_DATA,
                        PagingState.REFRESH -> PagingState.REFRESH
                    }
                    return state.copy(chatList = newChatList, pagingState = newPagingState)
                }
                is Action.AddRecipients -> {
                    val index = state.chatList.indexOfFirst { it.id == action.chatId }
                    if (index < 0) return state
                    val chat = state.chatList[index]
                    val newUsers = action.newRecipients
                        .map { contact ->
                            User(
                                id = contact.id,
                                name = contact.name,
                                avatar = contact.avatar,
                                role = null,
                                lastReadMessageId = null,
                                lastMentionMessageId = null
                            )
                        }
                        .filter { newUser ->
                            chat.users.find { it.id == newUser.id } == null //не нашли в имеющихся пользователях
                        }
                    val editedChatList = state.chatList.toMutableList().apply {
                        set(index, chat.copy(users = chat.users + newUsers))
                    }
                    return state.copy(chatList = editedChatList)
                }
                is Action.DeleteRecipients -> {
                    val index = state.chatList.indexOfFirst { it.id == action.chatId }
                    if (index < 0) return state
                    val chat = state.chatList[index]
                    val users = chat.users.filter { !action.deletedUserIds.contains(it.id) }
                    val editedChatList = state.chatList.toMutableList().apply {
                        set(index, chat.copy(users = users))
                    }
                    return state.copy(chatList = editedChatList)
                }
                is Action.LeaveChat -> {
                    val index = state.chatList.indexOfFirst { it.id == action.chat.id }
                    if (index < 0) return state
                    val editedChatList = state.chatList.toMutableList().apply {
                        removeAt(index)
                    }
                    sideEffectListener.invoke(SideEffect.LoadPage(1))
                    val newPagingState = when (state.pagingState) {
                        PagingState.EMPTY,
                        PagingState.EMPTY_ERROR,
                        PagingState.EMPTY_PROGRESS -> PagingState.EMPTY_PROGRESS
                        PagingState.DATA,
                        PagingState.NEW_PAGE_PROGRESS,
                        PagingState.FULL_DATA,
                        PagingState.REFRESH -> PagingState.REFRESH
                    }
                    return state.copy(
                        chatList = editedChatList,
                        pagingState = newPagingState
                    )
                }
                is Action.ExternalChatArchived -> {
                    val chatList = state.chatList.filter { it.id != action.chat.id }
                    sideEffectListener.invoke(SideEffect.LoadPage(1))
                    val newPagingState = when (state.pagingState) {
                        PagingState.EMPTY,
                        PagingState.EMPTY_ERROR,
                        PagingState.EMPTY_PROGRESS -> PagingState.EMPTY_PROGRESS
                        PagingState.DATA,
                        PagingState.NEW_PAGE_PROGRESS,
                        PagingState.FULL_DATA,
                        PagingState.REFRESH -> PagingState.REFRESH
                    }
                    return state.copy(
                        chatList = chatList,
                        pagingState = newPagingState
                    )
                }
            }
        }
    }
}
