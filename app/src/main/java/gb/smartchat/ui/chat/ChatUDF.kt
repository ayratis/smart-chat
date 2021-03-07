package gb.smartchat.ui.chat

import android.net.Uri
import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.data.download.DownloadStatus
import gb.smartchat.entity.File
import gb.smartchat.entity.Message
import gb.smartchat.entity.ReadInfo
import gb.smartchat.utils.toQuotedMessage
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.time.*
import java.util.*
import kotlin.math.max

object ChatUDF {

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
        data class ServerMessagesDeleted(val messages: List<Message>) : Action()
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
        data class ServerLoadReadInfoSuccess(val readInfo: ReadInfo) : Action()
        data class ServerLoadReadInfoError(val throwable: Throwable) : Action()
        data class ServerUploadFileSuccess(val file: File) : Action()
        data class ServerUploadFileError(val throwable: Throwable) : Action()
        data class ServerLoadBottomSuccess(val messages: List<Message>) : Action()
        data class ServerLoadBottomError(val throwable: Throwable) : Action()

        object InternalLoadMoreUpMessages : Action()
        object InternalLoadMoreDownMessages : Action()
        data class InternalTypingTimeIsUp(val senderId: String) : Action()
        data class InternalConnected(val isOnline: Boolean) : Action()
        data class InternalAtBottom(val atBottom: Boolean) : Action()
        data class InternalItemBind(val chatItem: ChatItem) : Action()
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
        data class FakeScrollToCenter(val position: Int, val isUp: Boolean) : SideEffect()
        object FakeScrollToBottom : SideEffect()
        data class LoadReadInfo(val fromMessageId: Long) : SideEffect()
        data class UploadFile(val contentUri: Uri) : SideEffect()
        object CancelUploadFile : SideEffect()
        data class DownloadFile(val message: Message) : SideEffect()
        data class CancelDownloadFile(val message: Message) : SideEffect()
        data class OpenFile(val contentUri: Uri) : SideEffect()
        data class ReadMessage(val messageId: Long) : SideEffect()
        object LoadBottomMessages : SideEffect()
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
        val atBottom: Boolean = true,
        val sendEnabled: Boolean = false,
        val readInfo: ReadInfo,
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
        SPECIFIC_PART_PROGRESS,
        BOTTOM_PROGRESS,
        READ_INFO_PROGRESS
    }

    class Store(private val senderId: String, readInfo: ReadInfo) : ObservableSource<State>,
        Consumer<Action>, Disposable {

        companion object {
            private const val TAG = "store"
        }

        private val actions = PublishRelay.create<Action>()
        private val state = BehaviorRelay.createDefault(State(readInfo = readInfo))
        var sideEffectListener: (SideEffect) -> Unit = {}

        private val disposable: Disposable = actions.hide()
            .subscribe { action ->
                val newState = reduce(state.value!!, action) {
                    sideEffectListener.invoke(it)
                }
                state.accept(newState)
                Log.d(TAG, "action: $action")
                Log.d(TAG, "state: $newState")
            }

        private fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
            when (action) {
                //adds items in list
                is Action.ClientActionWithMessage -> {
                    //if editing existing message
                    if (state.editingMessage != null) {
                        sideEffectListener(SideEffect.SetInputText(""))
                        sideEffectListener(
                            SideEffect.EditMessage(state.editingMessage, state.currentText)
                        )
                        val newMessage = state.editingMessage.copy(text = state.currentText)
                        val newItem = newMessage.mapIntoChatItem(state.readInfo)
                        return state.copy(
                            chatItems = state.chatItems.replaceLastMsgWith(newItem),
                            editingMessage = null
                        )
                    }
                    //if sending new message
                    if (state.attachmentState is AttachmentState.Uploading ||
                        state.currentText.isBlank() && state.attachmentState is AttachmentState.Empty
                    ) return state

                    val instant = Instant.now()
                    val file = (state.attachmentState as? AttachmentState.UploadSuccess)?.file
                    val msg = Message(
                        id = -1,
                        chatId = 1,
                        senderId = senderId,
                        clientId = instant.toEpochMilli().toString(),
                        text = state.currentText,
                        type = null,
                        quotedMessage = state.quotingMessage?.toQuotedMessage(),
                        timeCreated = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()),
                        file = file
                    )
                    sideEffectListener.invoke(SideEffect.SendMessage(msg))
                    sideEffectListener(SideEffect.SetInputText(""))
                    val newMessageItem = ChatItem.Msg.Outgoing(msg, ChatItem.OutgoingStatus.SENDING)
                    sideEffectListener(SideEffect.InstantScrollTo(state.chatItems.lastIndex))
                    return if (state.fullDataDown) state.copy(
                        chatItems = state.chatItems.plusMsg(newMessageItem),
                        currentText = "",
                        quotingMessage = null,
                        sendEnabled = false,
                        attachmentState = AttachmentState.Empty,
                    )
                    else {
                        state.copy(
                            chatItems = emptyList<ChatItem>().plusMsg(newMessageItem),
                            currentText = "",
                            quotingMessage = null,
                            fullDataDown = true,
                            pagingState = PagingState.DATA,
                            sendEnabled = false,
                            attachmentState = AttachmentState.Empty
                        )
                    }
                }
                is Action.ServerMessageNew -> {
                    if (state.fullDataDown) {
                        val newItem = action.message.mapIntoChatItem(state.readInfo)
                        val list = if (newItem is ChatItem.Msg.Outgoing) {
                            state.chatItems.replaceLastMsgWith(newItem)
                        } else {
                            state.chatItems.plusMsg(newItem)
                        }

                        if (newItem is ChatItem.Msg.Outgoing) {
                            return if (state.atBottom) {
                                sideEffectListener(SideEffect.InstantScrollTo(state.chatItems.lastIndex))
                                state.copy(chatItems = list)
                            } else {
                                //todo fake scroll down
                                state.copy(chatItems = list)
                            }
                        } else {
                            return if (state.atBottom) {
                                sideEffectListener(SideEffect.InstantScrollTo(state.chatItems.lastIndex))
                                val readInfo = state.readInfo.copy(
                                    readIn = newItem.message.id,
                                    unreadCount = 0
                                )
                                state.copy(
                                    chatItems = list,
                                    readInfo = readInfo
                                )
                            } else {
                                val readInfo = state.readInfo.copy(
                                    unreadCount = state.readInfo.unreadCount + 1
                                )
                                state.copy(
                                    chatItems = list,
                                    readInfo = readInfo
                                )
                            }
                        }
                    } else {
                        val readInfo = state.readInfo.copy(
                            unreadCount = state.readInfo.unreadCount + 1
                        )
                        return state.copy(readInfo = readInfo)
                    }
                }
                is Action.ServerMessageNewPage -> {
                    val newItems = action.items.mapIntoChatItems(state.readInfo)
                    when (state.pagingState) {
                        PagingState.EMPTY_PROGRESS -> {
                            return if (newItems.isEmpty()) {
                                state.copy(
                                    chatItems = emptyList(),
                                    pagingState = PagingState.EMPTY
                                )
                            } else {
                                state.copy(
                                    chatItems = newItems,
                                    pagingState = PagingState.DATA
                                )
                            }
                        }
                        PagingState.NEW_PAGE_UP_PROGRESS -> {
                            return if (action.items.isEmpty()) {
                                state.copy(pagingState = PagingState.DATA, fullDataUp = true)
                            } else {
                                state.copy(
                                    chatItems = newItems.plusList(state.chatItems),
                                    pagingState = PagingState.DATA
                                )
                            }
                        }
                        PagingState.NEW_PAGE_DOWN_PROGRESS -> {
                            return if (action.items.isEmpty()) {
                                state.copy(pagingState = PagingState.DATA, fullDataDown = true)
                            } else {
                                state.copy(
                                    chatItems = state.chatItems.plusList(newItems),
                                    pagingState = PagingState.DATA
                                )
                            }
                        }
                        PagingState.NEW_PAGE_UP_DOWN_PROGRESS -> {
                            val currentId = state.chatItems.firstMsg().message.id
                            val isUpProgress = currentId == action.fromMessageId
                            if (isUpProgress) {
                                return if (action.items.isEmpty()) {
                                    state.copy(
                                        pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS,
                                        fullDataUp = true
                                    )
                                } else {
                                    state.copy(
                                        chatItems = newItems.plusList(state.chatItems),
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
                                        chatItems = state.chatItems.plusList(newItems),
                                        pagingState = PagingState.NEW_PAGE_UP_PROGRESS
                                    )
                                }
                            }
                        }
                        else -> return state
                    }
                }
                is Action.ServerSpecificPartSuccess -> {
                    val newItems = action.items.mapIntoChatItems(state.readInfo)
                    val targetPosition = newItems.indexOfFirst {
                        it is ChatItem.Msg && it.message.id == action.targetMessageId
                    }
                    val isUp =
                        newItems.firstMsg().message.id < state.chatItems.firstMsg().message.id
                    sideEffectListener(SideEffect.FakeScrollToCenter(targetPosition, isUp))
                    return state.copy(
                        chatItems = newItems,
                        pagingState = PagingState.DATA,
                        fullDataUp = false,
                        fullDataDown = false,
                    )
                }
                is Action.ServerLoadBottomSuccess -> {
                    if (state.pagingState == PagingState.BOTTOM_PROGRESS) {
                        val newItems = action.messages.mapIntoChatItems(state.readInfo)
                        return if (newItems.isEmpty()) {
                            state.copy(
                                chatItems = emptyList(),
                                pagingState = PagingState.EMPTY,
                                readInfo = state.readInfo.copy(unreadCount = 0)
                            )
                        } else {
                            sideEffectListener(SideEffect.FakeScrollToBottom)
                            state.copy(
                                chatItems = newItems,
                                pagingState = PagingState.DATA,
                                fullDataDown = true,
                                readInfo = state.readInfo.copy(
                                    readIn = newItems.lastMsg().message.id,
                                    unreadCount = 0
                                ),
                            )
                        }
                    }
                    return state
                }

                //paging and scrolling actions, but no actions with list
                is Action.InternalConnected -> {
                    if (action.isOnline) {
                        return when (state.pagingState) {
                            PagingState.EMPTY, PagingState.EMPTY_ERROR -> {
                                val fromMessageId =
                                    if (state.readInfo.readIn == -1L) null
                                    else state.readInfo.readIn + 1
                                sideEffectListener(SideEffect.LoadPage(fromMessageId, false))
                                state.copy(
                                    isOnline = true,
                                    pagingState = PagingState.EMPTY_PROGRESS
                                )
                            }
                            else -> {
                                sideEffectListener(SideEffect.LoadReadInfo(state.readInfo.readIn))
                                state.copy(
                                    isOnline = true,
                                    pagingState = PagingState.READ_INFO_PROGRESS
                                )
                            }
                        }
                    }
                    return state.copy(isOnline = action.isOnline)
                }
                is Action.ServerLoadReadInfoSuccess -> {
                    return if (state.readInfo.readIn == action.readInfo.readIn) {
                        state.copy(
                            readInfo = action.readInfo,
                            fullDataDown = false,
                            pagingState = PagingState.DATA
                        )
                    } else {
                        sideEffectListener(SideEffect.LoadReadInfo(state.readInfo.readIn))
                        state
                    }
                }
                is Action.InternalLoadMoreUpMessages -> {
                    return when (state.pagingState) {
                        PagingState.DATA -> {
                            val fromMessageId = state.chatItems.firstMsg().message.id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, false))
                            state.copy(pagingState = PagingState.NEW_PAGE_UP_PROGRESS)
                        }
                        PagingState.NEW_PAGE_DOWN_PROGRESS -> {
                            val fromMessageId = state.chatItems.firstMsg().message.id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, false))
                            state.copy(pagingState = PagingState.NEW_PAGE_UP_DOWN_PROGRESS)
                        }
                        else -> state
                    }
                }
                is Action.InternalLoadMoreDownMessages -> {
                    return when (state.pagingState) {
                        PagingState.DATA -> {
                            val fromMessageId = state.chatItems.lastMsg().message.id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, true))
                            state.copy(pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS)
                        }
                        PagingState.NEW_PAGE_UP_PROGRESS -> {
                            val fromMessageId = state.chatItems.lastMsg().message.id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, true))
                            state.copy(pagingState = PagingState.NEW_PAGE_UP_DOWN_PROGRESS)
                        }
                        else -> state
                    }
                }
                is Action.ClientScrollToMessage -> {
                    val position = state.chatItems.indexOfLast {
                        it is ChatItem.Msg && it.message.id == action.messageId
                    }
                    return if (position != -1) {
                        sideEffectListener(SideEffect.InstantScrollTo(position))
                        state
                    } else {
                        sideEffectListener(SideEffect.LoadSpecificPart(action.messageId))
                        state.copy(pagingState = PagingState.SPECIFIC_PART_PROGRESS)
                    }
                }
                is Action.ClientScrollToBottom -> {
                    return if (state.fullDataDown) {
                        sideEffectListener(SideEffect.InstantScrollTo(state.chatItems.lastIndex))
                        val readInfo = state.readInfo.copy(
                            readIn = state.chatItems.lastMsg().message.id,
                            unreadCount = 0
                        )
                        state.copy(readInfo = readInfo)
                    } else {
                        sideEffectListener(SideEffect.LoadBottomMessages)
                        state.copy(pagingState = PagingState.BOTTOM_PROGRESS)
                    }
                }
                is Action.ClientEmptyRetry -> {
                    sideEffectListener(SideEffect.LoadPage(null, false))
                    return state.copy(
                        pagingState = PagingState.EMPTY_PROGRESS,
                        fullDataDown = false
                    )
                }
                is Action.ServerLoadReadInfoError -> {
                    sideEffectListener(SideEffect.PageErrorEvent(action.throwable))
                    return state.copy(pagingState = PagingState.DATA)
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
                            val currentId = state.chatItems.firstMsg().message.id
                            val isUpProgress = currentId == action.fromMessageId
                            val newPagingState =
                                if (isUpProgress) PagingState.NEW_PAGE_DOWN_PROGRESS
                                else PagingState.NEW_PAGE_UP_PROGRESS
                            state.copy(pagingState = newPagingState)
                        }
                        else -> state
                    }
                }
                is Action.ServerSpecificPartError -> {
                    //todo показать диалог
                    return state.copy(pagingState = PagingState.DATA)
                }
                is Action.ServerLoadBottomError -> {
                    //todo показать диалог
                    return state.copy(pagingState = PagingState.DATA)
                }

                //just replacing item in list
                is Action.ServerMessageSendSuccess -> {
                    val newItem =
                        ChatItem.Msg.Outgoing(action.message, ChatItem.OutgoingStatus.SENT)
                    return state.copy(chatItems = state.chatItems.replaceLastMsgWith(newItem))
                }
                is Action.ServerMessageSendError -> {
                    val newItem =
                        ChatItem.Msg.Outgoing(action.message, ChatItem.OutgoingStatus.FAILURE)
                    return state.copy(chatItems = state.chatItems.replaceLastMsgWith(newItem))
                }
                is Action.ServerMessageChange -> {
                    Log.d(TAG, "reduce: Action.ServerMessageChange, $action")
                    val newItem = action.message.mapIntoChatItem(state.readInfo)
                    return state.copy(chatItems = state.chatItems.replaceLastMsgWith(newItem))
                }
                is Action.ServerMessageRead -> {
                    val newReadOut = action.messageIds.maxOf { it }
                    val readInfo = state.readInfo.copy(readOut = newReadOut)
                    val chatItems = state.chatItems.map { chatItem ->
                        if (chatItem is ChatItem.Msg.Outgoing) {
                            when (chatItem.status) {
                                ChatItem.OutgoingStatus.SENDING,
                                ChatItem.OutgoingStatus.FAILURE,
                                ChatItem.OutgoingStatus.SENT,
                                ChatItem.OutgoingStatus.READ -> {
                                    return@map chatItem
                                }
                                ChatItem.OutgoingStatus.SENT_2 -> {
                                    return@map if (chatItem.message.id > readInfo.readOut) chatItem
                                    else chatItem.copy(status = ChatItem.OutgoingStatus.READ)
                                }
                            }
                        } else {
                            chatItem
                        }
                    }
                    return state.copy(chatItems = chatItems, readInfo = readInfo)
                }
                is Action.ServerMessagesDeleted -> {
                    val chatItems = state.chatItems.toMutableList()
                    val deletedMessages = action.messages.toMutableList()
                    for (i in chatItems.lastIndex downTo 0) {
                        if (deletedMessages.isEmpty()) {
                            break
                        }
                        val chatItem = chatItems[i]
                        if (chatItem !is ChatItem.Msg) continue
                        val deletedMessage = deletedMessages.find { it.id == chatItem.message.id }
                        if (deletedMessage != null) {
                            chatItems[i] = deletedMessage.mapIntoChatItem(state.readInfo)
                            deletedMessages.remove(deletedMessage)
                        }
                    }
                    return state.copy(chatItems = chatItems)
                }
                is Action.ServerMessageEditSuccess -> {
                    val newItem = action.message.mapIntoChatItem(state.readInfo)
                    return state.copy(chatItems = state.chatItems.replaceLastMsgWith(newItem))
                }
                is Action.ServerMessageEditError -> {
                    //todo показать диалог
                    val newItem = action.message.mapIntoChatItem(state.readInfo)
                    return state.copy(chatItems = state.chatItems.replaceLastMsgWith(newItem))
                }
                is Action.ServerMessageDeleteSuccess -> {
                    val newItem = action.message.mapIntoChatItem(state.readInfo)
                    return state.copy(chatItems = state.chatItems.replaceLastMsgWith(newItem))
                }

                //no actions with list
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
                is Action.InternalAtBottom -> {
                    return if (state.fullDataDown && action.atBottom) {
                        state.copy(
                            atBottom = true,
                            readInfo = state.readInfo.copy(unreadCount = 0)
                        )
                    } else {
                        state.copy(atBottom = false)
                    }
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
                is Action.ClientDeleteMessage -> {
                    if (action.message.senderId != senderId) return state
                    sideEffectListener(SideEffect.DeleteMessage(action.message))
                    return state
                }
                is Action.ServerMessageDeleteError -> {
                    //todo показать диалог
                    return state
                }
                is Action.InternalItemBind -> {
                    if (action.chatItem is ChatItem.Msg &&
                        action.chatItem.message.senderId != senderId &&
                        action.chatItem.message.id > state.readInfo.readIn
                    ) {
                        sideEffectListener(SideEffect.ReadMessage(action.chatItem.message.id))
                        val readInfo = state.readInfo.copy(
                            readIn = action.chatItem.message.id,
                            unreadCount = max(state.readInfo.unreadCount - 1, 0)
                        )
                        return state.copy(readInfo = readInfo)
                    }
                    return state
                }
            }
        }

        private fun List<ChatItem>.replaceLastMsgWith(item: ChatItem.Msg): List<ChatItem> {
            return this.replaceLastWith(item) {
                if (it is ChatItem.Msg) {
                    if (it.message.senderId == senderId) {
                        return@replaceLastWith it.message.clientId == item.message.clientId
                    } else {
                        return@replaceLastWith it.message.id == item.message.id
                    }
                }
                false
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

        private fun List<ChatItem>.firstMsg(): ChatItem.Msg {
            return first { it is ChatItem.Msg } as ChatItem.Msg
        }

        private fun List<ChatItem>.lastMsg(): ChatItem.Msg {
            return last { it is ChatItem.Msg } as ChatItem.Msg
        }

        private fun List<Message>.mapIntoChatItems(readInfo: ReadInfo): List<ChatItem> {
            if (isEmpty()) return emptyList()
            val list = mutableListOf<ChatItem>()
            var localDate: LocalDate? = null
            this.forEach { message ->
                val msgLocalDate = message.timeCreated.toLocalDate()
                if (msgLocalDate != null && msgLocalDate != localDate) {
                    list += ChatItem.DateHeader(msgLocalDate)
                    localDate = msgLocalDate
                }
                list += message.mapIntoChatItem(readInfo)
            }
            return list
        }

        private fun Message.mapIntoChatItem(readInfo: ReadInfo): ChatItem.Msg {
            return when {
                senderId == this@Store.senderId -> {
                    val status =
                        if (id > readInfo.readOut) ChatItem.OutgoingStatus.SENT_2
                        else ChatItem.OutgoingStatus.READ
                    ChatItem.Msg.Outgoing(this, status)
                }
                (type == Message.Type.SYSTEM || type == Message.Type.DELETED) -> {
                    ChatItem.Msg.System(this)
                }
                else -> {
                    ChatItem.Msg.Incoming(this)
                }
            }
        }

        private fun List<ChatItem>.plusMsg(chatItemMsg: ChatItem.Msg): List<ChatItem> {
            val newItemDate = chatItemMsg.message.timeCreated.toLocalDate()
            if (this.isEmpty()) {
                return listOf(ChatItem.DateHeader(newItemDate), chatItemMsg)
            }
            val lastItem = this.last() as ChatItem.Msg //last message cant be dateHeader
            val lastItemDate = lastItem.message.timeCreated.toLocalDate()
            if (lastItemDate != newItemDate) {
                return this + ChatItem.DateHeader(newItemDate) + chatItemMsg
            }
            return this + chatItemMsg
        }

        private fun List<ChatItem>.plusList(anotherList: List<ChatItem>): List<ChatItem> {
            if (this.isEmpty()) {
                return anotherList
            }
            if (anotherList.isEmpty()) {
                return this
            }
            val endDate = (this.last() as ChatItem.Msg).message.timeCreated.toLocalDate()
            val startDate = (anotherList.first() as ChatItem.DateHeader).localDate

            return if (endDate == startDate) {
                this + anotherList.toMutableList().apply {
                    removeAt(0) //removing dateHeader
                }
            } else {
                this + anotherList
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

        override fun subscribe(observer: Observer<in State>) {
            state.hide().subscribe(observer)
        }
    }
}
