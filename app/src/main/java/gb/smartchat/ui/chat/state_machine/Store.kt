package gb.smartchat.ui.chat.state_machine

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.entity.Message
import gb.smartchat.ui.chat.ChatItem
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

class Store(private val senderId: String) : ObservableSource<State>, Consumer<Action>, Disposable {

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
            is Action.ClientSendMessage -> {
                return if (state.currentText.isNotEmpty()) {
                    val clientId = System.currentTimeMillis().toString()
                    val msg = createOutgoingMessage(clientId, state.currentText)
                    sideEffectListener.invoke(SideEffect.SendMessage(msg))
                    val list = state.chatItems + ChatItem.Outgoing(msg, ChatItem.OutgoingStatus.SENDING)
                    state.copy(chatItems = list, currentText = "")
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
                return if (action.message.senderId == senderId) {
                    val newItem = ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.SENT_2)
                    val list = state.chatItems.toMutableList().apply {
                        replaceOrAddToEnd(newItem) { chatItem ->
                            chatItem is ChatItem.Outgoing &&
                                    chatItem.message.clientId == action.message.clientId
                        }
                    }
                    state.copy(chatItems = list)
                } else {
                    val list = state.chatItems + ChatItem.Incoming(action.message)
                    state.copy(chatItems = list)
                }
            }
            is Action.ServerMessageChange -> {
                val position = state.chatItems.indexOfLast { it.message.id == action.message.id }
                if (position != -1) {
                    val newItem = when {
                        action.message.type == Message.Type.SYSTEM -> {
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
                    val newList = state.chatItems.toMutableList().apply {
                        this[position] = newItem
                    }
                    return state.copy(chatItems = newList)
                }
                return state
            }
            is Action.ServerTyping -> {
                val typingSenderIds = state.typingSenderIds + action.senderId
                return state.copy(typingSenderIds = typingSenderIds)
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
                        val newItem = (chatItem as? ChatItem.Outgoing)
                            ?.copy(status = ChatItem.OutgoingStatus.READ)
                            ?: chatItem
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
                return state.copy(editingMessage = action.message)
            }
            is Action.ClientEditMessageConfirm -> {
                sideEffectListener(SideEffect.EditMessage(action.message, action.newText))
                val position = state.chatItems.indexOfLast { it.message.id == action.message.id }
                if (position != -1) {
                    val newList = state.chatItems.toMutableList()
                    val newMessage = action.message.copy(text = action.newText)
                    val newItem = ChatItem.Outgoing(newMessage, ChatItem.OutgoingStatus.EDITING)
                    newList[position] = newItem
                    return state.copy(chatItems = newList, editingMessage = null)
                }
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
        }
    }

    private fun createOutgoingMessage(clientId: String, text: String): Message {
        return Message(
            id = -1,
            chatId = 1,
            senderId = senderId,
            clientId = clientId,
            text = text,
            type = null,
            readedIds = emptyList()
        )
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