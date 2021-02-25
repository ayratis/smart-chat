package gb.smartchat.ui.chat

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.socket.SocketApi
import gb.smartchat.data.socket.SocketEvent
import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageReadRequest
import gb.smartchat.entity.request.TypingRequest
import gb.smartchat.ui.chat.state_machine.Action
import gb.smartchat.ui.chat.state_machine.SideEffect
import gb.smartchat.ui.chat.state_machine.State
import gb.smartchat.ui.chat.state_machine.Store
import gb.smartchat.utils.ContentUriRequestBody
import gb.smartchat.utils.SingleEvent
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import java.util.concurrent.TimeUnit

class ChatViewModel(
    private val store: Store,
    private val userId: String,
    private val chatId: Long,
    private val socketApi: SocketApi,
    private val httpApi: HttpApi,
) : ViewModel() {

    class Factory(
        private val userId: String,
        private val chatId: Long,
        private val socketApi: SocketApi,
        private val httpApi: HttpApi,
    ) : ViewModelProvider.Factory {

        private val store = Store(userId)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChatViewModel(store, userId, chatId, socketApi, httpApi) as T
        }
    }

    companion object {
        const val TAG = "ChatViewModel"
    }

    private val compositeDisposable = CompositeDisposable()
    private val typingTimersDisposableMap = HashMap<String, Disposable>()
    val viewState = BehaviorRelay.create<State>()
    val setInputText = BehaviorRelay.create<SingleEvent<String>>()
    val instaScrollTo = PublishRelay.create<Int>()

    init {
        setupStateMachine()
        observeSocketEvents()
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        typingTimersDisposableMap.values.forEach { d ->
            if (!d.isDisposed) {
                d.dispose()
            }
        }
        socketApi.disconnect()
    }

    private fun setupStateMachine() {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is SideEffect.SendMessage -> sendMessage(sideEffect.message)
                is SideEffect.TypingTimer -> startTypingTimer(sideEffect.senderId)
                is SideEffect.EditMessage -> editMessage(sideEffect.message, sideEffect.newText)
                is SideEffect.DeleteMessage -> deleteMessage(sideEffect.message)
                is SideEffect.SetInputText -> setInputText.accept(SingleEvent(sideEffect.text))
                is SideEffect.LoadPage -> fetchPage(sideEffect.fromMessageId, sideEffect.forward)
                is SideEffect.PageErrorEvent -> {
                    Log.e(TAG, "PageErrorEvent", sideEffect.throwable)
                }
                is SideEffect.LoadSpecificPart -> loadSpecificPart(sideEffect.fromMessageId)
                is SideEffect.InstaScrollTo -> instaScrollTo.accept(sideEffect.position)
                is SideEffect.LoadNewMessages -> loadNewMessages(sideEffect.fromMessageId)
            }
        }
        compositeDisposable.add(
            Observable.wrap(store)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    viewState.accept(state)
                }
        )
        compositeDisposable.add(
            Observable.wrap(store)
                .map { it.currentText }
                .distinctUntilChanged()
                .filter { it.isNotEmpty() }
                .throttleLatest(1500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { text ->
                    sendTyping(text)
                }
        )
    }

    private fun observeSocketEvents() {
        val d = socketApi.observeEvents()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when (event) {
                    is SocketEvent.Connected -> {
                        store.accept(Action.InternalConnected(true))
                    }
                    is SocketEvent.Disconnected -> {
                        store.accept(Action.InternalConnected(false))
                    }
                    is SocketEvent.MessageNew -> {
                        store.accept(Action.ServerMessageNew(event.message))
                    }
                    is SocketEvent.MessageChange -> {
                        store.accept(Action.ServerMessageChange(event.message))
                    }
                    is SocketEvent.Typing -> {
                        store.accept(Action.ServerTyping(event.senderId))
                    }
                    is SocketEvent.MessageRead -> {
                        store.accept(Action.ServerMessageRead(event.messageIds))
                    }
                }
            }
        compositeDisposable.add(d)
    }

    private fun sendMessage(message: Message) {
        val messageCreateRequest = message.toMessageCreateRequestBody() ?: return
        val d = socketApi.sendMessage(messageCreateRequest)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    if (result) store.accept(Action.ServerMessageSendSuccess(message))
                    else store.accept(Action.ServerMessageSendError(message))
                },
                { e ->
                    Log.e(TAG, "sendMessage: error", e)
                    store.accept(Action.ServerMessageSendError(message))
                }
            )
        compositeDisposable.add(d)
    }

    private fun editMessage(message: Message, newText: String) {
        val messageEditRequest = message.copy(text = newText).toMessageEditRequestBody() ?: return
        val d = socketApi.editMessage(messageEditRequest)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    if (result) {
                        store.accept(Action.ServerMessageEditSuccess(message.copy(text = newText)))
                    } else {
                        store.accept(Action.ServerMessageEditSuccess(message))
                    }
                },
                { e ->
                    Log.e(TAG, "sendMessage: error", e)
                    store.accept(Action.ServerMessageEditSuccess(message))
                }
            )
        compositeDisposable.add(d)
    }

    private fun deleteMessage(message: Message) {
        val messageDeleteRequest = message.toMessageDeleteRequestBody() ?: return
        val d = socketApi.deleteMessage(messageDeleteRequest)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    if (result) {
                        store.accept(Action.ServerMessageDeleteSuccess(message))
                    } else {
                        store.accept(Action.ServerMessageDeleteError(message))
                    }
                },
                { e ->
                    Log.e(TAG, "deleteMessage: error", e)
                    store.accept(Action.ServerMessageDeleteError(message))
                }
            )
        compositeDisposable.add(d)
    }

    private fun startTypingTimer(senderId: String) {
        typingTimersDisposableMap[senderId]?.dispose()
        typingTimersDisposableMap[senderId] = Completable
            .timer(2, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                store.accept(Action.InternalTypingTimeIsUp(senderId))
            }
    }

    private fun sendTyping(text: String) {
        val requestBody = TypingRequest(
            chatId = chatId,
            senderId = userId,
            text = text
        )
        val d = socketApi.sendTyping(requestBody)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /*do nothing*/ },
                { Log.e(TAG, "sendTyping: error", it) }
            )
        compositeDisposable.addAll(d)
    }

    private fun readMessage(message: Message) {
        val requestBody = MessageReadRequest(
            messageIds = listOf(message.id),
            chatId = chatId,
            senderId = userId,
        )
        val d = socketApi.readMessage(requestBody)
            .subscribe(
                { /*do nothing*/
                    Log.d(TAG, "onChatItemBind: success")
                },
                { e ->
                    Log.e(TAG, "messageRead", e)
                }
            )
        compositeDisposable.add(d)
    }

    private fun fetchPage(fromMessageId: Long?, forward: Boolean) {
        val messageId = if (fromMessageId == -1L) null else fromMessageId
        val d = httpApi
            .getChatMessageHistory(
                chatId = chatId,
                pageSize = State.DEFAULT_PAGE_SIZE,
                messageId = messageId,
                lookForward = forward
            )
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data ->
                    store.accept(Action.ServerMessageNewPage(data, messageId))
                },
                { e ->
                    store.accept(Action.ServerMessagePageError(e, messageId))
                }
            )
        compositeDisposable.add(d)
    }

    private fun loadSpecificPart(fromMessageId: Long) {
        val d = httpApi
            .getChatMessageHistory(
                chatId = chatId,
                pageSize = State.DEFAULT_PAGE_SIZE,
                messageId = fromMessageId + (State.DEFAULT_PAGE_SIZE / 2),
                lookForward = false
            )
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(Action.ServerSpecificPartSuccess(it, fromMessageId)) },
                { store.accept(Action.ServerSpecificPartError(it)) }
            )
        compositeDisposable.add(d)
    }

    private fun loadNewMessages(fromMessageId: Long) {
        val d = httpApi
            .getChatMessageHistory(
                chatId = chatId,
                pageSize = State.DEFAULT_PAGE_SIZE,
                messageId = fromMessageId,
                lookForward = true
            )
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(Action.ServerLoadNewMessagesSuccess(it)) },
                { store.accept(Action.ServerLoadNewMessagesError(it)) }
            )
        compositeDisposable.add(d)
    }

    fun uploadFile(uri: Uri, contentResolver: ContentResolver) {
        val fileBody = ContentUriRequestBody(MediaType.parse("*/*")!!, contentResolver, uri)
        val d = httpApi
            .postUploadFile(fileBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Log.d(TAG, "uploadFile: success: $it") },
                { Log.d(TAG, "uploadFile: error", it) }
            )
        compositeDisposable.add(d)
    }

    fun onStart() {
        socketApi.connect()
    }

    fun onTextChanged(text: String) {
        Log.d(TAG, "onTextChanged: $text")
        store.accept(Action.ClientTextChanged(text))
    }

    fun onSendClick() {
        store.accept(Action.ClientActionWithMessage)
    }

    fun onEditMessageRequest(message: Message) {
        store.accept(Action.ClientEditMessageRequest(message))
    }

    fun onEditMessageReject() {
        store.accept(Action.ClientEditMessageReject)
    }

    fun onDeleteMessage(message: Message) {
        store.accept(Action.ClientDeleteMessage(message))
    }

    fun attachPhoto(photoUri: Uri) {
        store.accept(Action.ClientAttachPhoto(photoUri))
    }

    fun detachPhoto() {
        store.accept(Action.ClientDetachPhoto)
    }

    fun attachFile(fileUri: Uri) {
        store.accept(Action.ClientAttachFile(fileUri))
    }

    fun detachFile() {
        store.accept(Action.ClientDetachFile)
    }

    fun onQuoteMessage(message: Message) {
        store.accept(Action.ClientQuoteMessage(message))
    }

    fun stopQuoting() {
        store.accept(Action.ClientStopQuoting)
    }

    fun onChatItemBind(chatItem: ChatItem) {
        Log.d(TAG, "onChatItemBind: $chatItem")
        if (chatItem !is ChatItem.Outgoing &&
            chatItem.message.readedIds?.contains(userId) != true
        ) {
            readMessage(chatItem.message)
        }
    }

    fun loadNextPage(forward: Boolean) {
        store.accept(
            if (forward) Action.InternalLoadMoreDownMessages
            else Action.InternalLoadMoreUpMessages
        )
    }

    fun onQuotedMessageClick(chatItem: ChatItem) {
        chatItem.message.quotedMessage?.messageId?.let {
            store.accept(Action.ClientScrollToMessage(it))
        }
    }

    fun atBottomOfChat(atBottom: Boolean) {
        store.accept(Action.InternalAtBottom(atBottom))
    }

    fun scrollToBottom() {
        store.accept(Action.ScrollToBottom)
    }

    fun emptyRetry() {
        store.accept(Action.EmptyRetry)
    }
}