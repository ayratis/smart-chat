package gb.smartchat.library.ui.chat

import android.net.Uri
import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.BuildConfig
import gb.smartchat.library.data.download.DownloadStatus
import gb.smartchat.library.entity.*
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.composeWithMessage
import gb.smartchat.library.utils.toQuotedMessage
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.lang.Integer.max
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
        data class ServerMessageEdited(val changedMessage: ChangedMessage) : Action()
        data class ServerTyping(val senderId: String) : Action()
        data class ServerMessageRead(val messageIds: List<Long>) : Action()
        data class ServerMessagesDeleted(val messages: List<Message>) : Action()
        data class ServerMessageSendSuccess(val message: Message) : Action()
        data class ServerMessageSendError(val message: Message) : Action()
        data class ServerMessageEditSuccess(val message: Message) : Action()
        data class ServerMessageEditError(val message: Message) : Action()
        data class ServerMessageDeleteSuccess(val message: Message) : Action()
        data class ServerMessageDeleteError(val message: Message) : Action()
        data class ServerMessageNewPage(
            val messages: List<Message>,
            val fromMessageId: Long?
        ) : Action()

        data class ServerMessagePageError(
            val throwable: Throwable,
            val fromMessageId: Long?
        ) : Action()

        data class ServerSpecificPartSuccess(
            val messages: List<Message>,
            val targetMessageId: Long
        ) : Action()

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
        data class ReadMessage(val message: Message) : Action()
        data class InternalUpdateMessage(val message: Message) : Action()
        data class ClientMentionClick(val mention: User) : Action()

        data class ServerAddRecipients(val newRecipients: List<Contact>) : Action()
        data class ServerDeleteRecipients(val deletedUserIds: List<String>) : Action()
    }

    sealed class SideEffect {
        data class SendMessage(val message: Message) : SideEffect()
        data class TypingTimer(val senderId: String) : SideEffect()
        data class EditMessage(val oldMessage: Message, val newMessage: Message) : SideEffect()
        data class DeleteMessage(val message: Message) : SideEffect()
        data class SetInputText(val text: String) : SideEffect()
        data class LoadPage(val fromMessageId: Long?, val forward: Boolean) : SideEffect()
        data class PageErrorEvent(val throwable: Throwable) : SideEffect()
        data class LoadSpecificPart(val fromMessageId: Long) : SideEffect()
        data class LoadReadInfo(val fromMessageId: Long) : SideEffect()
        data class UploadFile(val contentUri: Uri) : SideEffect()
        object CancelUploadFile : SideEffect()
        data class DownloadFile(val message: Message) : SideEffect()
        data class CancelDownloadFile(val message: Message) : SideEffect()
        data class OpenFile(val contentUri: Uri) : SideEffect()
        data class ReadMessage(val messageId: Long, val newUnreadCount: Int) : SideEffect()
        object LoadBottomMessages : SideEffect()
    }

    data class State(
        val chat: Chat,
        val users: List<User>,
        val readInfo: ReadInfo,
        val messages: List<Message> = emptyList(),
        val draft: List<Message> = emptyList(),
        val pagingState: PagingState = PagingState.EMPTY,
        val currentText: String = "",
        val editingMessage: Message? = null,
        val quotingMessage: Message? = null,
        val attachmentState: AttachmentState = AttachmentState.Empty,
        val isOnline: Boolean = false,
        val fullDataUp: Boolean = false,
        val fullDataDown: Boolean = false,
        val atBottom: Boolean = true,
        val sendEnabled: Boolean = false,
        val typingSenderIds: List<String> = emptyList(),
        val withScrollTo: SingleEvent<WithScrollTo>? = null,
        val mentions: List<User> = emptyList()
    ) {

        override fun toString(): String {
            if (BuildConfig.DEBUG) {
                return "State(" +
//                    "chat=$chat, " +
//                    "users=$users," +
//                    " readInfo=$readInfo, " +
//                    "messages=$messages, " +
//                    "draft=$draft, " +
                        "pagingState=$pagingState, " +
//                    "currentText='$currentText', " +
//                    "editingMessage=$editingMessage, " +
//                    "quotingMessage=$quotingMessage, " +
//                    "attachmentState=$attachmentState, " +
                        "isOnline=$isOnline, " +
                        "fullDataUp=$fullDataUp, " +
                        "fullDataDown=$fullDataDown, " +
                        "atBottom=$atBottom, " +
//                    "sendEnabled=$sendEnabled, " +
//                    "typingSenderIds=$typingSenderIds, " +
                        "withScrollTo=$withScrollTo, " +
//                    "mentions=$mentions" +
                        ")"
            }
            return super.toString()
        }
    }

    data class WithScrollTo(
        val message: Message,
        val fake: Boolean = false,
        val isUp: Boolean = false
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

    class Store(
        private val userId: String,
        private val chat: Chat
    ) : ObservableSource<State>, Consumer<Action>, Disposable {

        companion object {
            private const val TAG = "ChatUDF"
        }

        private val actions = PublishRelay.create<Action>()
        private val state =
            BehaviorRelay.createDefault(State(chat, chat.users, chat.getReadInfo(userId)))
        var sideEffectListener: (SideEffect) -> Unit = {}

        private val disposable: Disposable = actions.hide()
            .subscribe { action ->
                Log.d(TAG, "action: $action")
                val newState = reduce(state.value!!, action) {
                    Log.d(TAG, "sideEffect: $it")
                    sideEffectListener.invoke(it)
                }
                Log.d(TAG, "state: $newState")
                state.accept(newState)
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
                        val newMessage = state.editingMessage.copy(
                            text = state.currentText.trim(),
                            mentions = state.currentText.trim().getMentions(state.users)
                        )
                        sideEffectListener(
                            SideEffect.EditMessage(state.editingMessage, newMessage)
                        )
                        return state.copy(
                            messages = state.messages.replaceLastWith(newMessage),
                            editingMessage = null,
                            currentText = "",
                            sendEnabled = false,
                            mentions = emptyList(),
                            attachmentState = AttachmentState.Empty,
                        )
                    }
                    //if sending new message
                    if (state.attachmentState is AttachmentState.Uploading ||
                        state.currentText.isBlank() && state.attachmentState is AttachmentState.Empty
                    ) return state

                    val instant = Instant.now()
                    val file = (state.attachmentState as? AttachmentState.UploadSuccess)?.file
                    val message = Message(
                        id = -1,
                        chatId = chat.id,
                        senderId = userId,
                        clientId = instant.toEpochMilli().toString(),
                        text = state.currentText.trim(),
                        type = null,
                        quotedMessage = state.quotingMessage?.toQuotedMessage(),
                        timeCreated = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()),
                        timeUpdated = null,
                        file = file,
                        mentions = state.currentText.getMentions(state.users),
                        chatName = chat.name,
                        senderName = state.users.find { it.id == userId }?.name
                    )
                    sideEffectListener.invoke(SideEffect.SendMessage(message))
                    sideEffectListener(SideEffect.SetInputText(""))
                    val withScrollTo = if (state.fullDataDown) {
                        SingleEvent(WithScrollTo(message))
                        // sideEffectListener(SideEffect.InstantScrollTo(state.messages.lastIndex))
                    } else {
                        null
                    }
                    return state.copy(
                        draft = state.draft + message,
                        currentText = "",
                        quotingMessage = null,
                        sendEnabled = false,
                        attachmentState = AttachmentState.Empty,
                        mentions = emptyList(),
                        withScrollTo = withScrollTo
                    )
                }
                is Action.ServerMessageNew -> {
                    val draft = if (action.message.isOutgoing(userId)) {
                        state.draft.filter { it.clientId != action.message.clientId }
                    } else {
                        state.draft
                    }

                    if (state.pagingState == PagingState.READ_INFO_PROGRESS) {
                        return state.copy(draft = draft)
                    }

                    if (state.fullDataDown && (
                                state.pagingState == PagingState.DATA ||
                                        state.pagingState == PagingState.NEW_PAGE_UP_PROGRESS
                                )
                    ) {
                        val readInfo = if (action.message.isOutgoing(userId) || state.atBottom) {
                            state.readInfo.copy(unreadCount = 0)
                        } else {
                            state.readInfo.copy(unreadCount = state.readInfo.unreadCount + 1)
                        }
                        val withScrollTo =
                            if (action.message.isOutgoing(userId) || state.atBottom) {
                                SingleEvent(WithScrollTo(action.message))
                            } else {
                                null
                            }
                        return state.copy(
                            messages = state.messages + action.message,
                            draft = draft,
                            readInfo = readInfo,
                            withScrollTo = withScrollTo
                        )
                    } else {
                        val readInfo = state.readInfo.copy(
                            unreadCount = state.readInfo.unreadCount + 1
                        )
                        return state.copy(
                            readInfo = readInfo,
                            draft = draft
                        )
                    }
                }
                is Action.ServerMessageNewPage -> {
                    when (state.pagingState) {
                        PagingState.EMPTY_PROGRESS -> {
                            return if (action.messages.isEmpty()) {
                                state.copy(
                                    messages = emptyList(),
                                    pagingState = PagingState.EMPTY
                                )
                            } else {
                                state.copy(
                                    messages = action.messages,
                                    pagingState = PagingState.DATA,
                                    fullDataDown = action.fromMessageId == null
                                )
                            }
                        }
                        PagingState.NEW_PAGE_UP_PROGRESS -> {
                            return if (action.messages.isEmpty()) {
                                state.copy(
                                    pagingState = PagingState.DATA,
                                    fullDataUp = true
                                )
                            } else {
                                state.copy(
                                    messages = action.messages + state.messages,
                                    pagingState = PagingState.DATA
                                )
                            }
                        }
                        PagingState.NEW_PAGE_DOWN_PROGRESS -> {
                            return if (action.messages.isEmpty()) {
                                state.copy(
                                    pagingState = PagingState.DATA,
                                    fullDataDown = true
                                )
                            } else {
                                state.copy(
                                    messages = state.messages + action.messages,
                                    pagingState = PagingState.DATA
                                )
                            }
                        }
                        PagingState.NEW_PAGE_UP_DOWN_PROGRESS -> {
                            val currentId = state.messages.first().id
                            val isUpProgress = currentId == action.fromMessageId
                            if (isUpProgress) {
                                return if (action.messages.isEmpty()) {
                                    state.copy(
                                        pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS,
                                        fullDataUp = true
                                    )
                                } else {
                                    state.copy(
                                        messages = action.messages + state.messages,
                                        pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS
                                    )
                                }
                            } else {
                                return if (action.messages.isEmpty()) {
                                    state.copy(
                                        pagingState = PagingState.NEW_PAGE_UP_PROGRESS,
                                        fullDataDown = true
                                    )
                                } else {
                                    state.copy(
                                        messages = state.messages + action.messages,
                                        pagingState = PagingState.NEW_PAGE_UP_PROGRESS
                                    )
                                }
                            }
                        }
                        else -> return state
                    }
                }
                is Action.ServerSpecificPartSuccess -> {
                    val message = action.messages.first { it.id == action.targetMessageId }
                    val isUp = action.messages.first().id < state.messages.first().id
                    val withScrollTo = SingleEvent(WithScrollTo(message, fake = true, isUp))
                    return state.copy(
                        messages = action.messages,
                        pagingState = PagingState.DATA,
                        fullDataUp = false,
                        fullDataDown = false,
                        withScrollTo = withScrollTo
                    )
                }
                is Action.ServerLoadBottomSuccess -> {
                    if (state.pagingState == PagingState.BOTTOM_PROGRESS) {
                        return if (action.messages.isEmpty()) {
                            state.copy(
                                messages = emptyList(),
                                pagingState = PagingState.EMPTY,
                                readInfo = state.readInfo.copy(unreadCount = 0)
                            )
                        } else {
                            val withScrollTo = SingleEvent(
                                WithScrollTo(
                                    action.messages.last(),
                                    fake = true,
                                    isUp = false
                                )
                            )
                            state.copy(
                                messages = action.messages,
                                pagingState = PagingState.DATA,
                                fullDataDown = true,
                                readInfo = state.readInfo.copy(unreadCount = 0),
                                withScrollTo = withScrollTo
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
                                    if (state.readInfo.readIn == -1L || state.readInfo.unreadCount == 0) null
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
                    //todo возможно нужен будет цикл if (state.readInfo.readIn == action.readInfo.readIn)
                    return if (state.fullDataDown) {
                        sideEffectListener(SideEffect.LoadPage(state.messages.last().id, true))
                        state.copy(
                            readInfo = action.readInfo,
                            fullDataDown = false,
                            pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS
                        )
                    } else {
                        state.copy(
                            readInfo = action.readInfo,
                            fullDataDown = false,
                            pagingState = PagingState.DATA
                        )
                    }
                }
                is Action.InternalLoadMoreUpMessages -> {
                    return when (state.pagingState) {
                        PagingState.DATA -> {
                            val fromMessageId = state.messages.first().id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, false))
                            state.copy(pagingState = PagingState.NEW_PAGE_UP_PROGRESS)
                        }
                        PagingState.NEW_PAGE_DOWN_PROGRESS -> {
                            val fromMessageId = state.messages.first().id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, false))
                            state.copy(pagingState = PagingState.NEW_PAGE_UP_DOWN_PROGRESS)
                        }
                        else -> state
                    }
                }
                is Action.InternalLoadMoreDownMessages -> {
                    return when (state.pagingState) {
                        PagingState.DATA -> {
                            val fromMessageId = state.messages.last().id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, true))
                            state.copy(pagingState = PagingState.NEW_PAGE_DOWN_PROGRESS)
                        }
                        PagingState.NEW_PAGE_UP_PROGRESS -> {
                            val fromMessageId = state.messages.last().id
                            sideEffectListener(SideEffect.LoadPage(fromMessageId, true))
                            state.copy(pagingState = PagingState.NEW_PAGE_UP_DOWN_PROGRESS)
                        }
                        else -> state
                    }
                }
                is Action.ClientScrollToMessage -> {
                    val position = state.messages.indexOfLast { it.id == action.messageId }
                    return if (position != -1) {
                        val withScrollTo = SingleEvent(WithScrollTo(state.messages[position]))
                        state.copy(withScrollTo = withScrollTo)
                    } else {
                        sideEffectListener(SideEffect.LoadSpecificPart(action.messageId))
                        state.copy(pagingState = PagingState.SPECIFIC_PART_PROGRESS)
                    }
                }
                is Action.ClientScrollToBottom -> {
                    return if (state.fullDataDown) {
                        val withScrollTo = SingleEvent(WithScrollTo(state.messages.last()))
                        val readInfo = state.readInfo.copy(unreadCount = 0)
                        state.copy(readInfo = readInfo, withScrollTo = withScrollTo)
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
                            val currentId = state.messages.first().id
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
                    //todo изменить состояние отправки
//                    val draft = state.draft.filter { it.clientId != action.message.clientId }
//                    return state.copy(draft = draft)
                    return state
                }
                is Action.ServerMessageSendError -> {
                    //todo состояние ошибки для черновика
                    return state
                }
                is Action.ServerMessageEdited -> {
                    val targetMessage = state.messages.find { it.id == action.changedMessage.id }
                        ?: return state
                    val editedMessage = action.changedMessage.composeWithMessage(targetMessage)
                    return state.copy(messages = state.messages.replaceLastWith(editedMessage))
                }
                is Action.InternalUpdateMessage -> {
                    return state.copy(messages = state.messages.replaceLastWith(action.message))
                }
                is Action.ServerMessageRead -> {
                    val newReadOut = action.messageIds.maxOf { it }
                    val readInfo = state.readInfo.copy(readOut = newReadOut)
                    return state.copy(readInfo = readInfo)
                }
                is Action.ServerMessagesDeleted -> {
                    val chatItems = state.messages.toMutableList()
                    val deletedMessages = action.messages.toMutableList()
                    for (i in chatItems.lastIndex downTo 0) {
                        if (deletedMessages.isEmpty()) {
                            break
                        }
                        val chatItem = chatItems[i]
                        val deletedMessage = deletedMessages.find { it.id == chatItem.id }
                        if (deletedMessage != null) {
                            chatItems[i] = deletedMessage
                            deletedMessages.remove(deletedMessage)
                        }
                    }
                    return state.copy(messages = chatItems)
                }
                is Action.ServerMessageEditSuccess -> {
                    return state.copy(messages = state.messages.replaceLastWith(action.message))
                }
                is Action.ServerMessageEditError -> {
                    //todo показать диалог
                    return state.copy(messages = state.messages.replaceLastWith(action.message))
                }
                is Action.ServerMessageDeleteSuccess -> {
                    return state.copy(messages = state.messages.replaceLastWith(action.message))
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
                    val position = action.text.indexOfLast { it == '@' }
                    val mentions = when {
                        action.text.isBlank() -> emptyList()
                        position == action.text.lastIndex -> state.users
                        action.text[position + 1] == ' ' -> emptyList()
                        position != -1 -> {
                            val name = action.text.substring(position + 1)
                            if (name.isBlank()) emptyList()
                            else state.users.filter { it.name?.contains(name, true) == true }
                        }
                        else -> emptyList()
                    }
                    return state.copy(
                        currentText = action.text,
                        sendEnabled = sendEnabled,
                        mentions = mentions
                    )
                }
                is Action.ClientMentionClick -> {
                    val pos = state.currentText.indexOfLast { it == '@' }
                    if (pos == -1 || action.mention.name == null) return state
                    val startText = state.currentText.substring(0, pos + 1)
                    val newText = startText + action.mention.name + ' '
                    sideEffectListener(SideEffect.SetInputText(newText))
                    return state.copy(
                        currentText = newText,
                        mentions = emptyList(),
                        sendEnabled = true
                    )
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
                    if (action.message.senderId != userId) return state
                    sideEffectListener(SideEffect.DeleteMessage(action.message))
                    return state
                }
                is Action.ServerMessageDeleteError -> {
                    //todo показать диалог
                    return state
                }
                is Action.ReadMessage -> {
                    if (action.message.senderId != userId &&
                        action.message.id > state.readInfo.readIn
                    ) {
                        val newUnreadCount = max(state.readInfo.unreadCount - 1, 0)
                        sideEffectListener(
                            SideEffect.ReadMessage(
                                action.message.id,
                                newUnreadCount
                            )
                        )
                        val readInfo = state.readInfo.copy(
                            readIn = action.message.id,
                            unreadCount = newUnreadCount
                        )
                        return state.copy(readInfo = readInfo)
                    }
                    return state
                }
                is Action.ServerAddRecipients -> {
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
                            state.users.find { it.id == newUser.id } == null //не нашли в имеющихся пользователях
                        }
                    return state.copy(users = state.users + newUsers)
                }
                is Action.ServerDeleteRecipients -> {
                    val users = state.users.filter { !action.deletedUserIds.contains(it.id) }
                    return state.copy(users = users)
                }
            }
        }

        private fun List<Message>.replaceLastWith(item: Message): List<Message> {
            return this.replaceLastWith(item) { it.id == item.id }
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

        private fun String.getMentions(users: List<User>): List<Mention> {
            val mentions = mutableListOf<Mention>()
            users.forEach { user ->
                if (user.name != null) {
                    val targetText = "@${user.name}"
                    val targetLengthUtf8 = targetText.encodeToByteArray().size
                    var startIndex = 0
                    var s = this
                    while (s.contains(targetText, true)) {
                        val offset = this.indexOf(targetText, startIndex, true)
                        val offsetUtf8 = this.substring(0, offset).encodeToByteArray().size
                        mentions += Mention(
                            userId = user.id,
                            offsetUtf8 = offsetUtf8,
                            lengthUtf8 = targetLengthUtf8
                        )
                        startIndex = offset + targetText.length
                        s = this.substring(startIndex)
                    }
                }
            }
            return mentions
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
