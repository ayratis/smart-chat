package gb.smartchat.ui.chat.state_machine

import android.net.Uri
import gb.smartchat.entity.Message


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
    data class ServerMessageNewPage(val items: List<Message>, val fromMessageId: Long?) : Action()
    data class ServerMessagePageError(val throwable: Throwable, val fromMessageId: Long?) : Action()
    data class ServerSpecificPartSuccess(val items: List<Message>, val targetMessageId: Long) :
        Action()

    data class ServerSpecificPartError(val throwable: Throwable) : Action()
    data class ServerLoadNewMessagesSuccess(val items: List<Message>) : Action()
    data class ServerLoadNewMessagesError(val throwable: Throwable) : Action()
    data class ServerUploadFileSuccess(val fileId: Long) : Action()
    data class ServerUploadFileError(val throwable: Throwable) : Action()

    object InternalLoadMoreUpMessages : Action()
    object InternalLoadMoreDownMessages : Action()
    data class InternalTypingTimeIsUp(val senderId: String) : Action()
    data class InternalConnected(val isOnline: Boolean) : Action()
    data class InternalAtBottom(val atBottom: Boolean) : Action()
}
