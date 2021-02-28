package gb.smartchat.ui.chat

import android.net.Uri
import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.data.download.DownloadStatus
import gb.smartchat.entity.File
import gb.smartchat.entity.Message
import gb.smartchat.utils.toQuotedMessage
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.util.*

object ChatUDF {

    const val UNREAD_OVER_MAX_COUNT = -1
    const val DEFAULT_PAGE_SIZE = 30

    sealed class Action {
        object ClientActionWithMessage : Action()
        data class ClientEditMessageRequest(val message: Message) : Action()
        object ClientEditMessageReject : Action()
        data class ClientDeleteMessage(val message: Message) : Action()
        data class ClientTextChanged(val text: String) : Action()
        data class ClientAttach(val contentUri: Uri) : Action()
        object ClientDetach : Action()
        data class ClientQuoteMessage(val message: Message) : Action()
        object ClientStopQuoting : Action()
        data class ClientScrollToMessage(val messageId: Long) : Action()
        object ClientScrollToBottom : Action()
        object ClientEmptyRetry : Action()
        data class ClientFileClick(val message: Message) : Action()

        data class ServerMessageNew(val message: Message) : Action()
        data class ServerMessageChange(val message: Message) : Action()
        data class ServerTyping(val senderId: String) : Action()
        data class ServerMessageRead(val messageIds: List<Long>) : Action()
        data class ServerMessageSendSuccess(val message: Message) : Action()
        data class ServerMessageSendError(val message: Message) : Action()
        data class ServerMessageEditSuccess(val message: Message) : Action()
        data class ServerMessageEditError(val message: Message) : Action()
        data class ServerMessageDeleteSuccess(val message: Message) : Action()
        data class ServerMessageDeleteError(val message: Message) : Action()
        data class ServerMessageNewPage(val items: List<Message>, val fromMessageId: Long?) :
            Action()

        data class ServerMessagePageError(val throwable: Throwable, val fromMessageId: Long?) :
            Action()

        data class ServerSpecificPartSuccess(val items: List<Message>, val targetMessageId: Long) :
            Action()

        data class ServerSpecificPartError(val throwable: Throwable) : Action()
        data class ServerLoadNewMessagesSuccess(val items: List<Message>) : Action()
        data class ServerLoadNewMessagesError(val throwable: Throwable) : Action()
        data class ServerUploadFileSuccess(val file: File) : Action()
        data class ServerUploadFileError(val throwable: Throwable) : Action()

        object InternalLoadMoreUpMessages : Action()
        object InternalLoadMoreDownMessages : Action()
        data class InternalTypingTimeIsUp(val senderId: String) : Action()
        data class InternalConnected(val isOnline: Boolean) : Action()
        data class InternalAtBottom(val atBottom: Boolean) : Action()
    }

    sealed class SideEffect {
        data class SendMessage(val message: Message) : SideEffect()
        data class TypingTimer(val senderId: String) : SideEffect()
        data class EditMessage(val message: Message, val newText: String) : SideEffect()
        data class DeleteMessage(val message: Message) : SideEffect()
        data class SetInputText(val text: String) : SideEffect()
        data class LoadPage(val fromMessageId: Long?, val forward: Boolean) : SideEffect()
        data class PageErrorEvent(val throwable: Throwable) : SideEffect()
        data class LoadSpecificPart(val fromMessageId: Long) : SideEffect()
        data class InstantScrollTo(val position: Int) : SideEffect()
        data class FakeScrollTo(val position: Int, val isUp: Boolean) : SideEffect()
        data class LoadNewMessages(val fromMessageId: Long) : SideEffect()
        data class UploadFile(val contentUri: Uri) : SideEffect()
        object CancelUploadFile : SideEffect()
        data class DownloadFile(val message: Message) : SideEffect()
        data class CancelDownloadFile(val message: Message) : SideEffect()
        data class OpenFile(val contentUri: Uri) : SideEffect()
    }

    data class State(
        val chatItems: List<ChatItem> = emptyList(),
        val typingSenderIds: List<String> = emptyList(),
        val editingMessage: Message? = null,
        val currentText: String = "",
        val attachmentState: AttachmentState = AttachmentState.Empty,
        val quotingMessage: Message? = null,
        val pagingState: PagingState = PagingState.EMPTY,
        val isOnline: Boolean = false,
        val chatEnabled: Boolean = true,
        val fullDataUp: Boolean = false,
        val fullDataDown: Boolean = false,
        val unreadMessageCount: Int = 0,
        val lastMessageId: Long? = null,
        val atBottom: Boolean = true,
        val sendEnabled: Boolean = false
    )

    sealed class AttachmentState {
        object Empty : AttachmentState()
        data class Uploading(val uri: Uri) : AttachmentState()
        data class UploadSuccess(val uri: Uri, val file: File) : AttachmentState()
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

    class Store(private val senderId: String): Consumer<Action>, Disposable {

        companion object {
            private const val TAG = "store"
        }

        private val actions = PublishRelay.create<Action>()
        private val viewStateSubject = BehaviorRelay.createDefault(State())
        private val sideEffectsSubject = PublishRelay.create<SideEffect>()

        val viewState: Observable<State> = viewStateSubject.hide()
        val sideEffects: Observable<SideEffect> = sideEffectsSubject.hide()

        private val disposable: Disposable = actions.hide()
            .subscribe { action ->
                val newState = reduce(viewStateSubject.value!!, action) {
                    sideEffectsSubject.accept(it)
                }
                viewStateSubject.accept(newState)
//                if (action !is Action.ServerMessageRead) { //paging debug
//                    Log.d(TAG, "action: $action")
//                }
                Log.d(TAG, "action: $action")
//                Log.d(TAG, "state: $newState")
                Log.d(
                    TAG,
                    "pagingState: ${newState.pagingState}, fullDataUp: ${newState.fullDataUp}, fullDataDown: ${newState.fullDataDown}, atBottom: ${newState.atBottom}"
                ) //paging debug
            }

        private fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
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
                            val newItem =
                                ChatItem.Outgoing(newMessage, ChatItem.OutgoingStatus.EDITING)
                            newList[position] = newItem
                            return state.copy(chatItems = newList, editingMessage = null)
                        }
                        return state.copy(editingMessage = null)
                    }
                    //if sending new message
                    if (state.attachmentState is AttachmentState.Uploading ||
                        state.currentText.isBlank() && state.attachmentState is AttachmentState.Empty
                    ) return state

                    val currentTime = System.currentTimeMillis()
                    val file = (state.attachmentState as? AttachmentState.UploadSuccess)?.file
                    val msg = Message(
                        id = -1,
                        chatId = 1,
                        senderId = senderId,
                        clientId = currentTime.toString(),
                        text = state.currentText,
                        type = null,
                        readedIds = emptyList(),
                        quotedMessage = state.quotingMessage?.toQuotedMessage(),
                        timeCreated = Date(currentTime),
                        file = file
                    )
                    sideEffectListener.invoke(SideEffect.SendMessage(msg))
                    sideEffectListener(SideEffect.SetInputText(""))
                    val newMessageItem = ChatItem.Outgoing(msg, ChatItem.OutgoingStatus.SENDING)
                    sideEffectListener(SideEffect.InstantScrollTo(state.chatItems.lastIndex))
                    return if (state.fullDataDown) state.copy(
                        chatItems = state.chatItems + newMessageItem,
                        currentText = "",
                        quotingMessage = null,
                        sendEnabled = false,
                        attachmentState = AttachmentState.Empty,
                    )
                    else state.copy(
                        chatItems = listOf(newMessageItem),
                        currentText = "",
                        quotingMessage = null,
                        fullDataDown = true,
                        pagingState = PagingState.DATA,
                        sendEnabled = false,
                        attachmentState = AttachmentState.Empty
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
                                    ChatItem.Outgoing(
                                        action.message,
                                        ChatItem.OutgoingStatus.SENT_2
                                    )
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
                                unreadMessageCount = 0
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
                            if (state.unreadMessageCount == UNREAD_OVER_MAX_COUNT) {
                                UNREAD_OVER_MAX_COUNT
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
                    sideEffectListener(SideEffect.CancelUploadFile)
                    return state.copy(
                        editingMessage = action.message,
                        attachmentState = AttachmentState.Empty
                    )
                }
                is Action.ClientEditMessageReject -> {
                    sideEffectListener(SideEffect.SetInputText(""))
                    return state.copy(editingMessage = null)
                }
                is Action.ServerMessageEditSuccess -> {
                    val position =
                        state.chatItems.indexOfLast { it.message.id == action.message.id }
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
                    val position =
                        state.chatItems.indexOfLast { it.message.id == action.message.id }
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
                    val newItem =
                        ChatItem.Outgoing(action.message, ChatItem.OutgoingStatus.DELETING)
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
                    val sendEnabled = state.attachmentState is AttachmentState.UploadSuccess ||
                            action.text.isNotBlank() && state.attachmentState !is AttachmentState.Uploading
                    return state.copy(currentText = action.text, sendEnabled = sendEnabled)
                }
                is Action.ClientAttach -> {
                    if (state.editingMessage != null) return state
                    sideEffectListener(SideEffect.UploadFile(action.contentUri))
                    return state.copy(
                        attachmentState = AttachmentState.Uploading(action.contentUri),
                        sendEnabled = false
                    )
                }
                is Action.ClientDetach -> {
                    sideEffectListener(SideEffect.CancelUploadFile)
                    return state.copy(
                        attachmentState = AttachmentState.Empty,
                        sendEnabled = state.currentText.isNotBlank()
                    )
                }
                is Action.ServerUploadFileSuccess -> {
                    return when (state.attachmentState) {
                        is AttachmentState.Uploading -> state.copy(
                            attachmentState = AttachmentState.UploadSuccess(
                                state.attachmentState.uri,
                                action.file
                            ),
                            sendEnabled = true
                        )
                        else -> state
                    }
                }
                is Action.ServerUploadFileError -> {
                    return state.copy(
                        attachmentState = AttachmentState.Empty,
                        sendEnabled = state.currentText.isNotBlank()
                    )
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
                                    fullDataDown = true
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
                        sideEffectListener(SideEffect.InstantScrollTo(position))
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
                    val isUp = newItems.first().message.id < state.chatItems.first().message.id
                    sideEffectListener(SideEffect.FakeScrollTo(targetPosition, isUp))
                    return state.copy(
                        chatItems = newItems,
                        pagingState = PagingState.DATA,
                        fullDataUp = false,
                        fullDataDown = false,
                    )
                }
                is Action.ServerSpecificPartError -> {
                    sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                    return state
                }
                is Action.ServerLoadNewMessagesSuccess -> {
                    if (action.items.size < DEFAULT_PAGE_SIZE) {
                        return if (state.fullDataDown) {
                            val list = state.chatItems + action.items.mapIntoChatItems()
                            if (state.atBottom) {
                                state.copy(chatItems = list)
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
                                unreadMessageCount = UNREAD_OVER_MAX_COUNT
                            )
                        } else {
                            state.copy(unreadMessageCount = UNREAD_OVER_MAX_COUNT)
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
                        sideEffectListener(SideEffect.InstantScrollTo(state.chatItems.lastIndex))
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
                is Action.ClientFileClick -> {
                    return when (val status = action.message.file!!.downloadStatus) {
                        is DownloadStatus.Empty -> {
                            sideEffectListener(SideEffect.DownloadFile(action.message))
                            state
                        }
                        is DownloadStatus.Downloading -> {
                            sideEffectListener(SideEffect.CancelDownloadFile(action.message))
                            state
                        }
                        is DownloadStatus.Success -> {
                            sideEffectListener(SideEffect.OpenFile(status.contentUri))
                            state
                        }
                    }
                }
            }
        }

        private inline fun <T> List<T>.replaceLastWith(
            item: T,
            predicate: (T) -> Boolean
        ): List<T> {
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

        override fun dispose() {
            disposable.dispose()
        }

        override fun isDisposed(): Boolean {
            return disposable.isDisposed
        }
    }
}
