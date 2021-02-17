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
            Log.d("Store", "action: $action")
            val newState = reduce(viewState.value!!, action)
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
                return if (state.currentText.isNotEmpty()) {
                    val clientId = System.currentTimeMillis().toString()
                    val msg = Message(
                        id = -1,
                        chatId = 1,
                        senderId = senderId,
                        clientId = clientId,
                        text = state.currentText,
                        type = null,
                        readedIds = emptyList(),
                        quotedMessageId = state.quotingMessage?.id
                    )
                    sideEffectListener.invoke(SideEffect.SendMessage(msg))
                    val list =
                        state.chatItems + ChatItem.Outgoing(msg, ChatItem.OutgoingStatus.SENDING)
                    sideEffectListener(SideEffect.SetInputText(""))
                    state.copy(chatItems = list, currentText = "", quotingMessage = null, withScrollTo = SingleEvent(list.lastIndex + 12))
                } else {
                    state
                }
            }
            is Action.ServerMessageSendSuccess -> {
                val newItem = ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.SENT)
                val list = state.chatItems.toMutableList().apply {
                    replaceOrAddToEnd(newItem) { chatItem ->
                        chatItem is ChatItem.Outgoing &&
                                chatItem.message.clientId == action.message.clientId
                    }
                }
                return state.copy(chatItems = list)
            }
            is Action.ServerMessageSendError -> {
                val newItem = ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.FAILURE)
                val list = state.chatItems.toMutableList().apply {
                    replaceOrAddToEnd(newItem) { chatItem ->
                        chatItem is ChatItem.Outgoing &&
                                chatItem.message.clientId == action.message.clientId
                    }
                }
                return state.copy(chatItems = list)
            }
            is Action.ServerMessageNew -> {
                when {
                    action.message.senderId == senderId -> {
                        val newItem =
                            ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.SENT_2)
                        val list = state.chatItems.toMutableList().apply {
                            replaceOrAddToEnd(newItem) { chatItem ->
                                chatItem is ChatItem.Outgoing &&
                                        chatItem.message.clientId == action.message.clientId
                            }
                        }
                        return state.copy(chatItems = list)
                    }
                    (action.message.type == Message.Type.SYSTEM ||
                            action.message.type == Message.Type.DELETED) -> {
                        val list = state.chatItems + ChatItem.System(action.message)
                        return state.copy(chatItems = list)
                    }
                    else -> {
                        val list = state.chatItems + ChatItem.Incoming(action.message)
                        return state.copy(chatItems = list)
                    }
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
            is Action.ClientAttachPhoto -> {
                return state.copy(attachedPhoto = action.photoUri, attachedFile = null)
            }
            is Action.ClientDetachPhoto -> {
                return state.copy(attachedPhoto = null, attachedFile = null)
            }
            is Action.ClientAttachFile -> {
                return state.copy(attachedPhoto = null, attachedFile = action.fileUri)
            }
            is Action.ClientDetachFile -> {
                return state.copy(attachedPhoto = null, attachedFile = null)
            }
            is Action.ClientQuoteMessage -> {
                return state.copy(quotingMessage = action.message)
            }
            is Action.ClientStopQuoting -> {
                return state.copy(quotingMessage = null)
            }
            is Action.InternalRefreshHistory -> {
                sideEffectListener(SideEffect.LoadPage(null))
                val newPagingState = when (state.pagingState) {
                    PagingState.EMPTY -> PagingState.EMPTY_PROGRESS
                    PagingState.EMPTY_ERROR -> PagingState.EMPTY_PROGRESS
                    PagingState.DATA -> PagingState.REFRESH
                    PagingState.NEW_PAGE_PROGRESS -> PagingState.REFRESH
                    PagingState.FULL_DATA -> PagingState.REFRESH
                    else -> state.pagingState
                }
                return state.copy(pagingState = newPagingState)
            }
            is Action.InternalLoadMoreMessages -> {
                return when (state.pagingState) {
                    PagingState.DATA -> {
                        val fromMessageId = state.chatItems.firstOrNull()?.message?.id
                        sideEffectListener(SideEffect.LoadPage(fromMessageId))
                        val newPagingState = PagingState.NEW_PAGE_PROGRESS
                        state.copy(pagingState = newPagingState)

                    }
                    else -> state
                }
            }
            is Action.ServerMessageNewPage -> {
                val newItems = action.items.map { message ->
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
                when (state.pagingState) {
                    PagingState.EMPTY_PROGRESS -> {
                        return if (newItems.isEmpty()) {
                            state.copy(chatItems = emptyList(), pagingState = PagingState.EMPTY)
                        } else {
                            state.copy(chatItems = newItems, pagingState = PagingState.DATA)
                        }
                    }
                    PagingState.REFRESH -> {
                        return if (newItems.isEmpty()) {
                            state.copy(chatItems = emptyList(), pagingState = PagingState.EMPTY)
                        } else {
                            state.copy(chatItems = newItems, pagingState = PagingState.DATA)
                        }
                    }
                    PagingState.NEW_PAGE_PROGRESS -> {
                        return if (action.items.isEmpty()) {
                            state.copy(pagingState = PagingState.FULL_DATA)
                        } else {
                            state.copy(
                                chatItems = newItems + state.chatItems,
                                pagingState = PagingState.DATA
                            )
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
                    PagingState.REFRESH, PagingState.NEW_PAGE_PROGRESS -> {
                        sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                        state.copy(pagingState = PagingState.DATA)
                    }
                    else -> state
                }
            }
            is Action.InternalConnectionAvailable -> {
                if (action.isOnline) {
                    sideEffectListener(SideEffect.LoadPage(null))
                    return state.copy(isOnline = action.isOnline, pagingState = PagingState.REFRESH)
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
                val targetPosition = newItems.indexOfFirst { it.message.id == action.targetMessageId }
                return state.copy(
                    chatItems = newItems,
                    pagingState = PagingState.DATA,
                    withScrollTo = SingleEvent(targetPosition)
                )
            }
            is Action.ServerSpecificPartError -> {
                sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                return state
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