package gb.smartchat.ui.chat.state_machine

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.entity.Message
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.SingleEvent
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.util.*

class Store(private val senderId: String) : ObservableSource<State>, Consumer<Action>, Disposable {

    companion object {
        private const val TAG = "store"
    }

    private val actions = PublishRelay.create<Action>()
    private val viewState = BehaviorRelay.createDefault(State())
    var sideEffectListener: (SideEffect) -> Unit = {}

    private var disposable: Disposable = actions
        .hide()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { action ->
            if (action !is Action.ServerMessageRead) { //paging debug
                Log.d(TAG, "action: $action")
            }
//            Log.d(TAG, "action: $action")
            val newState = reduce(viewState.value!!, action)
            Log.d(
                TAG,
                "pagingState: ${newState.pagingState}, fullDataUp: ${newState.fullDataUp}, fullDataDown: ${newState.fullDataDown}"
            ) //paging debug
//            Log.d(TAG, "state: $newState")
            viewState.accept(newState)
        }

    private fun reduce(state: State, action: Action): State {
        when (action) {
            is Action.ClientActionWithMessage -> {
                //if editing existing message
                if (state.editingMessage != null) {
                    sideEffectListener(SideEffect.SetInputText(""))
                    sideEffectListener(
                        SideEffect.EditMessage(
                            state.editingMessage,
                            state.currentText
                        )
                    )
                    val position =
                        state.chatItems.indexOfLast { it.message.id == state.editingMessage.id }
                    if (position != -1) {
                        val newList = state.chatItems.toMutableList()
                        val newMessage = state.editingMessage.copy(text = state.currentText)
                        val newItem = ChatItem.Outgoing(newMessage, ChatItem.OutgoingStatus.EDITING)
                        newList[position] = newItem
                        return state.copy(chatItems = newList, editingMessage = null)
                    }
                    return state.copy(editingMessage = null)
                }
                //if sending new message
                if (state.currentText.isBlank()) return state

                val currentTime = System.currentTimeMillis()
                val msg = Message(
                    id = -1,
                    chatId = 1,
                    senderId = senderId,
                    clientId = currentTime.toString(),
                    text = state.currentText,
                    type = null,
                    readedIds = emptyList(),
                    quotedMessage = state.quotingMessage?.toQuotedMessage(),
                    timeCreated = Date(currentTime)
                )
                sideEffectListener.invoke(SideEffect.SendMessage(msg))
                sideEffectListener(SideEffect.SetInputText(""))
                val newMessageItem = ChatItem.Outgoing(msg, ChatItem.OutgoingStatus.SENDING)
                return if (state.fullDataDown) state.copy(
                    chatItems = state.chatItems + newMessageItem,
                    currentText = "",
                    quotingMessage = null,
                    withScrollTo = SingleEvent(state.chatItems.lastIndex + 1)
                )
                else state.copy(
                    chatItems = listOf(newMessageItem),
                    currentText = "",
                    quotingMessage = null,
                    fullDataDown = true,
                    pagingState = PagingState.DATA
                )
            }
            is Action.ServerMessageSendSuccess -> {
                val newItem = ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.SENT)
                val list = state.chatItems.replaceLastWith(newItem) { chatItem ->
                    chatItem.message.clientId == action.message.clientId
                }
                return state.copy(chatItems = list)
            }
            is Action.ServerMessageSendError -> {
                val newItem = ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.FAILURE)
                val list = state.chatItems.replaceLastWith(newItem) { chatItem ->
                    chatItem.message.clientId == action.message.clientId
                }
                return state.copy(chatItems = list)
            }
            is Action.ServerMessageNew -> {
                if (state.fullDataDown) {
                    val list = when {
                        action.message.senderId == senderId -> {
                            val newItem =
                                ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.SENT_2)
                            state.chatItems.replaceLastWith(newItem) { chatItem ->
                                chatItem.message.clientId == action.message.clientId
                            }
                        }
                        (action.message.type == Message.Type.SYSTEM ||
                                action.message.type == Message.Type.DELETED) -> {
                            state.chatItems + ChatItem.System(action.message)
                        }
                        else -> {
                            state.chatItems + ChatItem.Incoming(action.message)
                        }
                    }
                    return if (state.atBottom) {
                        state.copy(
                            chatItems = list,
                            lastMessageId = list.last().message.id,
                            unreadMessageCount = 0,
                            withScrollTo = SingleEvent(list.lastIndex)
                        )
                    } else {
                        state.copy(
                            chatItems = list,
                            lastMessageId = list.last().message.id,
                            unreadMessageCount = state.unreadMessageCount + 1
                        )
                    }
                } else {
                    val unreadMessageCount =
                        if (state.unreadMessageCount == State.UNREAD_OVER_MAX_COUNT) {
                            State.UNREAD_OVER_MAX_COUNT
                        } else {
                            state.unreadMessageCount + 1
                        }
                    return state.copy(unreadMessageCount = unreadMessageCount)
                }
            }
            is Action.ServerMessageChange -> {
                Log.d(TAG, "reduce: Action.ServerMessageChange, $action")
                val newItem = when {
                    action.message.type == Message.Type.SYSTEM -> {
                        ChatItem.System(action.message)
                    }
                    action.message.type == Message.Type.DELETED -> {
                        ChatItem.System(action.message)
                    }
                    action.message.senderId == senderId -> {
                        val status = if (action.message.readedIds.isNullOrEmpty()) {
                            ChatItem.OutgoingStatus.SENT_2
                        } else {
                            ChatItem.OutgoingStatus.READ
                        }
                        ChatItem.Outgoing(action.message, status)
                    }
                    else -> {
                        ChatItem.Incoming(action.message)
                    }
                }
                val newList = state.chatItems.replaceLastWith(newItem) {
                    it.message.id == action.message.id
                }
                return state.copy(chatItems = newList)
            }
            is Action.ServerTyping -> {
                sideEffectListener(SideEffect.TypingTimer(action.senderId))
                if (!state.typingSenderIds.contains(action.senderId)) {
                    val typingSenderIds = state.typingSenderIds + action.senderId
                    return state.copy(typingSenderIds = typingSenderIds)
                }
                return state
            }
            is Action.InternalTypingTimeIsUp -> {
                val typingSenderIds = state.typingSenderIds - action.senderId
                return state.copy(typingSenderIds = typingSenderIds)
            }
            is Action.ServerMessageRead -> {
                val chatItems = state.chatItems.toMutableList()
                val messageIds = action.messageIds.toMutableList()
                for (i in chatItems.lastIndex downTo 0) {
                    val chatItem = chatItems[i]
                    if (messageIds.contains(chatItem.message.id)) {
                        val newReadIds = (chatItem.message.readedIds ?: emptyList()) + senderId
                        val newMessage = chatItem.message.copy(readedIds = newReadIds)
                        val newItem = when (chatItem) {
                            is ChatItem.Incoming -> ChatItem.Incoming(newMessage)
                            is ChatItem.System -> ChatItem.System(newMessage)
                            is ChatItem.Outgoing ->
                                ChatItem.Outgoing(newMessage, ChatItem.OutgoingStatus.READ)
                        }
                        chatItems[i] = newItem
                        messageIds.remove(chatItem.message.id)
                        if (messageIds.isEmpty()) {
                            break
                        }
                    }
                }
                return state.copy(chatItems = chatItems)
            }
            is Action.ClientEditMessageRequest -> {
                sideEffectListener(SideEffect.SetInputText(action.message.text ?: ""))
                return state.copy(editingMessage = action.message)
            }
            is Action.ClientEditMessageReject -> {
                sideEffectListener(SideEffect.SetInputText(""))
                return state.copy(editingMessage = null)
            }
            is Action.ServerMessageEditSuccess -> {
                val position = state.chatItems.indexOfLast { it.message.id == action.message.id }
                if (position != -1) {
                    val newList = state.chatItems.toMutableList()
                    val newStatus = if (action.message.readedIds.isNullOrEmpty()) {
                        ChatItem.OutgoingStatus.SENT_2
                    } else {
                        ChatItem.OutgoingStatus.READ
                    }
                    val newItem = ChatItem.Outgoing(action.message, newStatus)
                    newList[position] = newItem
                    return state.copy(chatItems = newList)
                }
                return state
            }
            is Action.ServerMessageEditError -> {
                val position = state.chatItems.indexOfLast { it.message.id == action.message.id }
                if (position != -1) {
                    val newList = state.chatItems.toMutableList()
                    val newStatus = if (action.message.readedIds.isNullOrEmpty()) {
                        ChatItem.OutgoingStatus.SENT_2
                    } else {
                        ChatItem.OutgoingStatus.READ
                    }
                    val newItem = ChatItem.Outgoing(action.message, newStatus)
                    newList[position] = newItem
                    return state.copy(chatItems = newList)
                }
                return state
            }
            is Action.ClientDeleteMessage -> {
                sideEffectListener(SideEffect.DeleteMessage(action.message))
                val newItem = ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.DELETING)
                val list = state.chatItems.replaceLastWith(newItem) { chatItem ->
                    chatItem.message.id == action.message.id
                }
                return state.copy(chatItems = list)
            }
            is Action.ServerMessageDeleteSuccess -> {
                val newItem = ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.DELETED)
                val list = state.chatItems.replaceLastWith(newItem) { chatItem ->
                    chatItem.message.id == action.message.id
                }
                return state.copy(chatItems = list)
            }
            is Action.ServerMessageDeleteError -> {
                val newStatus = if (action.message.readedIds.isNullOrEmpty()) {
                    ChatItem.OutgoingStatus.SENT_2
                } else {
                    ChatItem.OutgoingStatus.READ
                }
                val newItem = ChatItem.Outgoing(action.message, newStatus)
                val list = state.chatItems.replaceLastWith(newItem) { chatItem ->
                    chatItem.message.id == action.message.id
                }
                return state.copy(chatItems = list)
            }
            is Action.ClientTextChanged -> {
                return state.copy(currentText = action.text)
            }
            is Action.ClientAttach -> {
                sideEffectListener(SideEffect.UploadFile(action.contentUri))
                return state.copy(
                    attachmentState = AttachmentState.Uploading(action.contentUri)
                )
            }
            is Action.ClientDetach -> {
                sideEffectListener(SideEffect.CancelUploadFile)
                return state.copy(attachmentState = AttachmentState.Empty)
            }
            is Action.ServerUploadFileSuccess -> {
                return when (state.attachmentState) {
                    is AttachmentState.Uploading -> state.copy(
                        attachmentState = AttachmentState.UploadSuccess(
                            state.attachmentState.uri,
                            action.fileId
                        )
                    )
                    else -> state
                }
            }
            is Action.ServerUploadFileError -> {
                return state.copy(attachmentState = AttachmentState.Empty)
            }
            is Action.ClientQuoteMessage -> {
                return state.copy(quotingMessage = action.message)
            }
            is Action.ClientStopQuoting -> {
                return state.copy(quotingMessage = null)
            }
            is Action.InternalLoadMoreUpMessages -> {
                return when (state.pagingState) {
                    PagingState.DATA -> {
                        val fromMessageId = state.chatItems.firstOrNull()?.message?.id
                        sideEffectListener(SideEffect.LoadPage(fromMessageId, false))
                        state.copy(pagingState = PagingState.NEW_PAGE_UP_PROGRESS)
                    }
                    PagingState.NEW_PAGE_DOWN_PROGRESS -> {
                        val fromMessageId = state.chatItems.firstOrNull()?.message?.id
                        sideEffectListener(SideEffect.LoadPage(fromMessageId, false))
                        state.copy(pagingState = PagingState.NEW_PAGE_UP_DOWN_PROGRESS)
                    }
                    else -> state
                }
            }
            is Action.InternalLoadMoreDownMessages -> {
                return when (state.pagingState) {
                    PagingState.DATA -> {
                        val fromMessageId = state.chatItems.lastOrNull()?.message?.id
                        sideEffectListener(SideEffect.LoadPage(fromMessageId, true))
                        state.copy(pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS)
                    }
                    PagingState.NEW_PAGE_UP_PROGRESS -> {
                        val fromMessageId = state.chatItems.lastOrNull()?.message?.id
                        sideEffectListener(SideEffect.LoadPage(fromMessageId, true))
                        state.copy(pagingState = PagingState.NEW_PAGE_UP_DOWN_PROGRESS)
                    }
                    else -> state
                }
            }
            is Action.ServerMessageNewPage -> {
                val newItems = action.items.mapIntoChatItems()
                when (state.pagingState) {
                    PagingState.EMPTY_PROGRESS -> {
                        return if (newItems.isEmpty()) {
                            state.copy(
                                chatItems = emptyList(),
                                pagingState = PagingState.EMPTY,
                                lastMessageId = null
                            )
                        } else {
                            state.copy(
                                chatItems = newItems,
                                pagingState = PagingState.DATA,
                                lastMessageId = newItems.last().message.id,
                                fullDataDown = true,
                                withScrollTo = SingleEvent(newItems.lastIndex)
                            )
                        }
                    }
                    PagingState.NEW_PAGE_UP_PROGRESS -> {
                        return if (action.items.isEmpty()) {
                            state.copy(pagingState = PagingState.DATA, fullDataUp = true)
                        } else {
                            state.copy(
                                chatItems = newItems + state.chatItems,
                                pagingState = PagingState.DATA
                            )
                        }
                    }
                    PagingState.NEW_PAGE_DOWN_PROGRESS -> {
                        return if (action.items.isEmpty()) {
                            state.copy(pagingState = PagingState.DATA, fullDataDown = true)
                        } else {
                            state.copy(
                                chatItems = state.chatItems + newItems,
                                pagingState = PagingState.DATA
                            )
                        }
                    }
                    PagingState.NEW_PAGE_UP_DOWN_PROGRESS -> {
                        val currentId = state.chatItems.first().message.id
                        val isUpProgress = currentId == action.fromMessageId
                        if (isUpProgress) {
                            return if (action.items.isEmpty()) {
                                state.copy(
                                    pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS,
                                    fullDataUp = true
                                )
                            } else {
                                state.copy(
                                    chatItems = newItems + state.chatItems,
                                    pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS
                                )
                            }
                        } else {
                            return if (action.items.isEmpty()) {
                                state.copy(
                                    pagingState = PagingState.NEW_PAGE_UP_PROGRESS,
                                    fullDataDown = true
                                )
                            } else {
                                state.copy(
                                    chatItems = state.chatItems + newItems,
                                    pagingState = PagingState.NEW_PAGE_UP_PROGRESS
                                )
                            }
                        }
                    }
                    else -> return state
                }
            }
            is Action.ServerMessagePageError -> {
                return when (state.pagingState) {
                    PagingState.EMPTY_PROGRESS -> {
                        state.copy(pagingState = PagingState.EMPTY_ERROR)
                    }
                    PagingState.NEW_PAGE_UP_PROGRESS,
                    PagingState.NEW_PAGE_DOWN_PROGRESS -> {
                        sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                        state.copy(pagingState = PagingState.DATA)
                    }
                    PagingState.NEW_PAGE_UP_DOWN_PROGRESS -> {
                        sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                        val currentId = state.chatItems.first().message.id
                        val isUpProgress = currentId == action.fromMessageId
                        val newPagingState =
                            if (isUpProgress) PagingState.NEW_PAGE_DOWN_PROGRESS
                            else PagingState.NEW_PAGE_UP_PROGRESS
                        state.copy(pagingState = newPagingState)
                    }
                    else -> state
                }
            }
            is Action.InternalConnected -> {
                if (action.isOnline) {
                    return when (state.pagingState) {
                        PagingState.EMPTY, PagingState.EMPTY_ERROR -> {
                            sideEffectListener(SideEffect.LoadPage(null, false))
                            state.copy(
                                isOnline = true,
                                pagingState = PagingState.EMPTY_PROGRESS,
                                fullDataDown = false
                            )
                        }
                        else -> {
                            if (state.lastMessageId != null) {
                                sideEffectListener(SideEffect.LoadNewMessages(state.lastMessageId))
                                state.copy(isOnline = true)
                            } else {
                                sideEffectListener(SideEffect.LoadPage(null, false))
                                state.copy(
                                    isOnline = true,
                                    pagingState = PagingState.EMPTY_PROGRESS,
                                    fullDataDown = false
                                )
                            }
                        }
                    }
                }
                return state.copy(isOnline = action.isOnline)
            }
            is Action.ClientScrollToMessage -> {
                val position = state.chatItems.indexOfLast { it.message.id == action.messageId }
                return if (position != -1) {
                    sideEffectListener(SideEffect.InstaScrollTo(position))
                    state
                } else {
                    sideEffectListener(SideEffect.LoadSpecificPart(action.messageId))
                    state
                }
            }
            is Action.ServerSpecificPartSuccess -> {
                val newItems = action.items.mapIntoChatItems()
                val targetPosition =
                    newItems.indexOfFirst { it.message.id == action.targetMessageId }
                return state.copy(
                    chatItems = newItems,
                    pagingState = PagingState.DATA,
                    fullDataUp = false,
                    fullDataDown = false,
                    withScrollTo = SingleEvent(targetPosition)
                )
            }
            is Action.ServerSpecificPartError -> {
                sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                return state
            }
            is Action.ServerLoadNewMessagesSuccess -> {
                if (action.items.size < State.DEFAULT_PAGE_SIZE) {
                    return if (state.fullDataDown) {
                        val list = state.chatItems + action.items.mapIntoChatItems()
                        if (state.atBottom) {
                            state.copy(
                                chatItems = list,
                                withScrollTo = SingleEvent(list.lastIndex)
                            )
                        } else {
                            state.copy(chatItems = list)
                        }
                    } else {
                        state.copy(unreadMessageCount = action.items.size)
                    }
                } else {
                    return if (state.fullDataDown) {
                        val list = state.chatItems + action.items.mapIntoChatItems()
                        state.copy(
                            chatItems = list,
                            fullDataDown = false,
                            unreadMessageCount = State.UNREAD_OVER_MAX_COUNT
                        )
                    } else {
                        state.copy(unreadMessageCount = State.UNREAD_OVER_MAX_COUNT)
                    }
                }
            }
            is Action.ServerLoadNewMessagesError -> {
                sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                return state
            }
            is Action.InternalAtBottom -> {
                return state.copy(atBottom = action.atBottom)
            }
            is Action.ClientScrollToBottom -> {
                return if (state.fullDataDown) {
                    sideEffectListener(SideEffect.InstaScrollTo(state.chatItems.lastIndex))
                    state.copy(unreadMessageCount = 0)
                } else {
                    sideEffectListener(SideEffect.LoadPage(null, false))
                    state.copy(
                        chatItems = emptyList(),
                        pagingState = PagingState.EMPTY_PROGRESS,
                        fullDataDown = false,
                        unreadMessageCount = 0
                    )
                }
            }
            is Action.ClientEmptyRetry -> {
                sideEffectListener(SideEffect.LoadPage(null, false))
                return state.copy(
                    pagingState = PagingState.EMPTY_PROGRESS,
                    fullDataDown = false
                )
            }
        }
    }

    private inline fun <T> MutableList<T>.replaceOrAddToEnd(item: T, predicate: (T) -> Boolean) {
        val position = this.indexOfLast(predicate)
        if (position != -1) {
            this[position] = item
        } else {
            this += item
        }
    }

    private inline fun <T> List<T>.replaceLastWith(item: T, predicate: (T) -> Boolean): List<T> {
        val list = this.toMutableList()
        val position = list.indexOfLast(predicate)
        if (position != -1) {
            list[position] = item
        }
        return list
    }

    private fun List<Message>.mapIntoChatItems(): List<ChatItem> {
        return map { message ->
            when {
                message.senderId == senderId -> {
                    val status =
                        if (message.readedIds.isNullOrEmpty()) ChatItem.OutgoingStatus.SENT_2
                        else ChatItem.OutgoingStatus.READ
                    ChatItem.Outgoing(message, status)
                }
                (message.type == Message.Type.SYSTEM ||
                        message.type == Message.Type.DELETED) -> {
                    ChatItem.System(message)
                }
                else -> {
                    ChatItem.Incoming(message)
                }
            }
        }
    }

    override fun accept(t: Action) {
        actions.accept(t)
    }

    override fun subscribe(observer: Observer<in State>) {
        viewState.hide().subscribe(observer)
    }

    override fun dispose() {
        disposable.dispose()
    }

    override fun isDisposed(): Boolean {
        return disposable.isDisposed
    }
}
