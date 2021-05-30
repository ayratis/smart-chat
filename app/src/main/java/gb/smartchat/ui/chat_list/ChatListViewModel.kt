package gb.smartchat.ui.chat_list

import android.util.Log
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.data.socket.SocketApi
import gb.smartchat.data.socket.SocketEvent
import gb.smartchat.entity.Chat
import gb.smartchat.entity.request.PinChatRequest
import gb.smartchat.publisher.*
import gb.smartchat.utils.SingleEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ChatListViewModel(
    private val isArchive: Boolean,
    private val store: ChatListUDF.Store,
    private val httpApi: HttpApi,
    private val socketApi: SocketApi,
    private val resourceManager: ResourceManager,
    chatCreatedPublisher: ChatCreatedPublisher,
    messageReadInternalPublisher: MessageReadInternalPublisher,
    chatUnreadMessageCountPublisher: ChatUnreadMessageCountPublisher,
    private val chatUnarchivePublisher: ChatUnarchivePublisher,
    leaveChatPublisher: LeaveChatPublisher,
    chatArchivePublisher: ChatArchivePublisher
) : ViewModel() {

    companion object {
        private const val TAG = "ChatListViewModel"
    }

    private val compositeDisposable = CompositeDisposable()
    private val showErrorMessageCommand = BehaviorRelay.create<SingleEvent<String>>()

    val viewState: Observable<ChatListUDF.State> = Observable.wrap(store)
    val showErrorMessage: Observable<SingleEvent<String>> = showErrorMessageCommand.hide()

    init {
        setupStateMachine()
        store.accept(ChatListUDF.Action.Refresh)
        compositeDisposable.add(store)
        observeSocketEvents()
        chatCreatedPublisher
            .subscribe { store.accept(ChatListUDF.Action.NewChatCreated(it)) }
            .also { compositeDisposable.add(it) }
        messageReadInternalPublisher
            .subscribe { store.accept(ChatListUDF.Action.ReadMessage(it)) }
            .also { compositeDisposable.add(it) }
        chatUnreadMessageCountPublisher
            .subscribe {
                store.accept(ChatListUDF.Action.ChatUnreadMessageCountChanged(it.first, it.second))
            }
            .also { compositeDisposable.add(it) }
        if (!isArchive) {
            chatUnarchivePublisher
                .subscribe { store.accept(ChatListUDF.Action.ChatUnarchived(it)) }
                .also { compositeDisposable.add(it) }
        }
        leaveChatPublisher
            .subscribe { store.accept(ChatListUDF.Action.LeaveChat(it)) }
            .also { compositeDisposable.add(it) }
        chatArchivePublisher
            .subscribe { store.accept(ChatListUDF.Action.ExternalChatArchived(it)) }
            .also { compositeDisposable.add(it) }
    }

    private fun setupStateMachine() {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatListUDF.SideEffect.ErrorEvent -> {
                    Log.d(TAG, "errorEvent", sideEffect.error)
                }
                is ChatListUDF.SideEffect.LoadPage -> {
                    fetchPage(sideEffect.pageCount)
                }
                is ChatListUDF.SideEffect.PinChat -> {
                    pinChatOnServer(sideEffect.chat, sideEffect.pin)
                }
                is ChatListUDF.SideEffect.ShowPinChatError -> {
                    val message = resourceManager.getString(
                        if (sideEffect.pin) R.string.pin_error
                        else R.string.unpin_error
                    )
                    showErrorMessageCommand.accept(SingleEvent(message))
                }
                is ChatListUDF.SideEffect.ArchiveChat -> {
                    archiveChatOnServer(sideEffect.chat, sideEffect.archive)
                }
                is ChatListUDF.SideEffect.ShowArchiveChatError -> {
                    val message = resourceManager.getString(
                        if (sideEffect.archive) R.string.archive_error
                        else R.string.unarchive_error
                    )
                    showErrorMessageCommand.accept(SingleEvent(message))
                }
            }
        }
    }

    private fun observeSocketEvents() {
        socketApi.observeEvents()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { socketEvent ->
                when (socketEvent) {
                    is SocketEvent.MessageChange -> {
                        store.accept(ChatListUDF.Action.ChangeMessage(socketEvent.changedMessage))
                    }
                    is SocketEvent.MessageNew -> {
                        store.accept(ChatListUDF.Action.NewMessage(socketEvent.message))
                    }
                    is SocketEvent.MessageRead -> {
                        store.accept(ChatListUDF.Action.ReadMessage(socketEvent.messageRead))
                    }
                    is SocketEvent.MessagesDeleted -> {
                        store.accept(ChatListUDF.Action.DeleteMessages(socketEvent.messages))
                    }
                    is SocketEvent.AddRecipients -> {
                        store.accept(
                            ChatListUDF.Action.AddRecipients(
                                socketEvent.addRecipientsResponse.chatId,
                                socketEvent.addRecipientsResponse.contacts
                            )
                        )
                    }
                    is SocketEvent.DeleteRecipients -> {
                        store.accept(
                            ChatListUDF.Action.DeleteRecipients(
                                socketEvent.deleteRecipientsResponse.chatId,
                                socketEvent.deleteRecipientsResponse.deletedUserIds
                            )
                        )
                    }
                    else -> {
                    }
                }
            }
            .also { compositeDisposable.add(it) }
    }

    private fun fetchPage(pageCount: Int) {
        httpApi
            .getChatList(
                pageCount = pageCount,
                pageSize = ChatListUDF.DEFAULT_PAGE_SIZE,
                fromArchive = isArchive
            )
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatListUDF.Action.NewPage(pageCount, it)) },
                { store.accept(ChatListUDF.Action.PageError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun pinChatOnServer(chat: Chat, pin: Boolean) {
        val requestBody = PinChatRequest(chat.id)
        val pinRequest =
            if (pin) httpApi.postPinChat(requestBody)
            else httpApi.postUnpinChat(requestBody)

        pinRequest
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                if (!it.result.success) throw RuntimeException("pin chat server error")
                it.result.success
            }
            .subscribe(
                { store.accept(ChatListUDF.Action.PinChatSuccess(chat, pin)) },
                { store.accept(ChatListUDF.Action.PinChatError(it, chat, pin)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun archiveChatOnServer(chat: Chat, archive: Boolean) {
        val requestBody = PinChatRequest(chat.id)
        val archiveRequest =
            if (archive) httpApi.postArchiveChat(requestBody)
            else httpApi.postUnarchiveChat(requestBody)

        archiveRequest
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                if (!it.result.success) throw RuntimeException("pin chat server error")
                it.result.success
            }
            .doOnSuccess {
                if (!archive) {
                    chatUnarchivePublisher.accept(chat)
                }
            }
            .subscribe(
                { store.accept(ChatListUDF.Action.ArchiveChatSuccess(chat, archive)) },
                { store.accept(ChatListUDF.Action.ArchiveChatError(it, chat, archive)) }
            )
            .also { compositeDisposable.add(it) }
    }

    fun onPinChatClick(chat: Chat, pin: Boolean) {
        store.accept(ChatListUDF.Action.PinChat(chat, pin))
    }

    fun onArchiveChatClick(chat: Chat, archive: Boolean) {
        store.accept(ChatListUDF.Action.ArchiveChat(chat, archive))
    }

    fun loadMore() {
        store.accept(ChatListUDF.Action.LoadMore)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
