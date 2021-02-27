package gb.smartchat.ui.chat

import android.net.Uri
import android.util.Log
import android.util.LongSparseArray
import androidx.core.util.forEach
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.data.content.ContentHelper
import gb.smartchat.data.download.FileDownloadHelper
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.socket.SocketApi
import gb.smartchat.data.socket.SocketEvent
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageReadRequest
import gb.smartchat.entity.request.TypingRequest
import gb.smartchat.utils.SingleEvent
import gb.smartchat.utils.composeWithDownloadStatus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody
import java.util.concurrent.TimeUnit


class ChatViewModel(
    private val store: ChatUDF.Store,
    private val userId: String,
    private val chat: Chat,
    private val socketApi: SocketApi,
    private val httpApi: HttpApi,
    private val contentHelper: ContentHelper,
    private val downloadHelper: FileDownloadHelper,
) : ViewModel() {

    companion object {
        const val TAG = "ChatViewModel"
    }

    private val compositeDisposable = CompositeDisposable()
    private val typingTimersDisposableMap = HashMap<String, Disposable>()
    private val downloadStatusDisposableMap = LongSparseArray<Disposable>()
    private var uploadDisposable: Disposable? = null
    val viewState: Observable<ChatUDF.State> =
        Observable.wrap(store).observeOn(AndroidSchedulers.mainThread())
    val setInputText = BehaviorRelay.create<SingleEvent<String>>()
    val instaScrollTo = PublishRelay.create<Int>()
    val openFile = BehaviorRelay.create<SingleEvent<Uri>>()

    init {
        setupStateMachine()
        viewState
            .map { it.currentText }
            .distinctUntilChanged()
            .filter { it.isNotEmpty() }
            .throttleLatest(1500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { sendTyping(it) }
            .also { compositeDisposable.add(it) }
        observeSocketEvents()
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        typingTimersDisposableMap.values.forEach { d ->
            if (!d.isDisposed) {
                d.dispose()
            }
        }
        downloadStatusDisposableMap.forEach { _, d ->
            if (!d.isDisposed) {
                d.dispose()
            }
        }
        socketApi.disconnect()
    }

    private fun setupStateMachine() {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatUDF.SideEffect.SendMessage -> {
                    sendMessage(sideEffect.message)
                }
                is ChatUDF.SideEffect.TypingTimer -> {
                    startTypingTimer(sideEffect.senderId)
                }
                is ChatUDF.SideEffect.EditMessage -> {
                    editMessage(sideEffect.message, sideEffect.newText)
                }
                is ChatUDF.SideEffect.DeleteMessage -> {
                    deleteMessage(sideEffect.message)
                }
                is ChatUDF.SideEffect.SetInputText -> {
                    setInputText.accept(SingleEvent(sideEffect.text))
                }
                is ChatUDF.SideEffect.LoadPage -> {
                    fetchPage(sideEffect.fromMessageId, sideEffect.forward)
                }
                is ChatUDF.SideEffect.PageErrorEvent -> {
                    Log.e(TAG, "PageErrorEvent", sideEffect.throwable)
                }
                is ChatUDF.SideEffect.LoadSpecificPart -> {
                    loadSpecificPart(sideEffect.fromMessageId)
                }
                is ChatUDF.SideEffect.InstaScrollTo -> {
                    instaScrollTo.accept(sideEffect.position)
                }
                is ChatUDF.SideEffect.LoadNewMessages -> {
                    loadNewMessages(sideEffect.fromMessageId)
                }
                is ChatUDF.SideEffect.CancelUploadFile -> {
                    uploadDisposable?.dispose()
                    uploadDisposable = null
                }
                is ChatUDF.SideEffect.UploadFile -> {
                    uploadFile(sideEffect.contentUri)
                }
                is ChatUDF.SideEffect.DownloadFile -> {
                    downloadMessageFile(sideEffect.message)
                }
                is ChatUDF.SideEffect.CancelDownloadFile -> {
                    cancelDownloadMessageFile(sideEffect.message)
                }
                is ChatUDF.SideEffect.OpenFile -> {
                    openFile.accept(SingleEvent(sideEffect.contentUri))
                }
            }
        }
    }

    private fun observeSocketEvents() {
        val d = socketApi.observeEvents()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when (event) {
                    is SocketEvent.Connected -> {
                        store.accept(ChatUDF.Action.InternalConnected(true))
                    }
                    is SocketEvent.Disconnected -> {
                        store.accept(ChatUDF.Action.InternalConnected(false))
                    }
                    is SocketEvent.MessageNew -> {
                        val msg = event.message.composeWithDownloadStatus(downloadHelper)
                        store.accept(ChatUDF.Action.ServerMessageNew(msg))
                    }
                    is SocketEvent.MessageChange -> {
                        val msg = event.message.composeWithDownloadStatus(downloadHelper)
                        store.accept(ChatUDF.Action.ServerMessageChange(msg))
                    }
                    is SocketEvent.Typing -> {
                        store.accept(ChatUDF.Action.ServerTyping(event.senderId))
                    }
                    is SocketEvent.MessageRead -> {
                        store.accept(ChatUDF.Action.ServerMessageRead(event.messageIds))
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
                    if (result) {
                        store.accept(ChatUDF.Action.ServerMessageSendSuccess(message))
                    } else {
                        store.accept(ChatUDF.Action.ServerMessageSendError(message))
                    }
                },
                { e ->
                    Log.e(TAG, "sendMessage: error", e)
                    store.accept(ChatUDF.Action.ServerMessageSendError(message))
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
                        val msg = message.copy(text = newText)
                        store.accept(ChatUDF.Action.ServerMessageEditSuccess(msg))
                    } else {
                        store.accept(ChatUDF.Action.ServerMessageEditSuccess(message))
                    }
                },
                { store.accept(ChatUDF.Action.ServerMessageEditError(message)) }
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
                        store.accept(ChatUDF.Action.ServerMessageDeleteSuccess(message))
                    } else {
                        store.accept(ChatUDF.Action.ServerMessageDeleteError(message))
                    }
                },
                { e ->
                    Log.e(TAG, "deleteMessage: error", e)
                    store.accept(ChatUDF.Action.ServerMessageDeleteError(message))
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
                store.accept(ChatUDF.Action.InternalTypingTimeIsUp(senderId))
            }
    }

    private fun sendTyping(text: String) {
        val requestBody = TypingRequest(
            chatId = chat.id,
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
            chatId = chat.id,
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
                chatId = chat.id,
                pageSize = ChatUDF.DEFAULT_PAGE_SIZE,
                messageId = messageId,
                lookForward = forward
            )
            .map { response ->
                response.result.map {
                    it.composeWithDownloadStatus(downloadHelper)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data ->
                    store.accept(ChatUDF.Action.ServerMessageNewPage(data, messageId))
                },
                { e ->
                    store.accept(ChatUDF.Action.ServerMessagePageError(e, messageId))
                }
            )
        compositeDisposable.add(d)
    }

    private fun loadSpecificPart(fromMessageId: Long) {
        val d = httpApi
            .getChatMessageHistory(
                chatId = chat.id,
                pageSize = ChatUDF.DEFAULT_PAGE_SIZE,
                messageId = fromMessageId + (ChatUDF.DEFAULT_PAGE_SIZE / 2),
                lookForward = false
            )
            .map { response ->
                response.result.map {
                    it.composeWithDownloadStatus(downloadHelper)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatUDF.Action.ServerSpecificPartSuccess(it, fromMessageId)) },
                { store.accept(ChatUDF.Action.ServerSpecificPartError(it)) }
            )
        compositeDisposable.add(d)
    }

    private fun loadNewMessages(fromMessageId: Long) {
        val d = httpApi
            .getChatMessageHistory(
                chatId = chat.id,
                pageSize = ChatUDF.DEFAULT_PAGE_SIZE,
                messageId = fromMessageId,
                lookForward = true
            )
            .map { response ->
                response.result.map {
                    it.composeWithDownloadStatus(downloadHelper)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatUDF.Action.ServerLoadNewMessagesSuccess(it)) },
                { store.accept(ChatUDF.Action.ServerLoadNewMessagesError(it)) }
            )
        compositeDisposable.add(d)
    }

    private fun uploadFile(contentUri: Uri) {
        uploadDisposable?.dispose()
        val (name, size) = contentHelper.nameSize(contentUri) ?: return
        val mimeType = contentHelper.mimeType(contentUri) ?: "*/*"
        Log.d(TAG, "uploadFile: name: $name, size: $size, mimeType: $mimeType")
        val filePart = MultipartBody.Part
            .createFormData("upload_file", name, contentHelper.requestBody(contentUri))
        httpApi
            .postUploadFile(filePart)
            .map { it.result.file }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatUDF.Action.ServerUploadFileSuccess(it)) },
                { store.accept(ChatUDF.Action.ServerUploadFileError(it)) }
            ).also {
                uploadDisposable = it
                compositeDisposable.add(it)
            }
    }

    private fun downloadMessageFile(message: Message) {
        val d = downloadStatusDisposableMap[message.id]
        if (d?.isDisposed == false) {
            d.dispose()
        }
        message.file?.url?.let { url ->
            downloadHelper.download(url)
                .subscribe { downloadStatus ->
                    val newMessage =
                        message.copy(file = message.file.copy(downloadStatus = downloadStatus))
                    store.accept(ChatUDF.Action.ServerMessageChange(newMessage))
                }.also {
                    downloadStatusDisposableMap.put(message.id, it)
                }
        }
    }

    private fun cancelDownloadMessageFile(message: Message) {
        message.file?.url?.let { downloadHelper.cancelDownload(it) }
    }

    fun onStart() {
        socketApi.connect()
    }

    fun onTextChanged(text: String) {
        Log.d(TAG, "onTextChanged: $text")
        store.accept(ChatUDF.Action.ClientTextChanged(text))
    }

    fun onSendClick() {
        store.accept(ChatUDF.Action.ClientActionWithMessage)
    }

    fun onEditMessageRequest(message: Message) {
        store.accept(ChatUDF.Action.ClientEditMessageRequest(message))
    }

    fun onEditMessageReject() {
        store.accept(ChatUDF.Action.ClientEditMessageReject)
    }

    fun onDeleteMessage(message: Message) {
        store.accept(ChatUDF.Action.ClientDeleteMessage(message))
    }

    fun attach(contentUri: Uri) {
        store.accept(ChatUDF.Action.ClientAttach(contentUri))
    }

    fun detach() {
        store.accept(ChatUDF.Action.ClientDetach)
    }

    fun onQuoteMessage(message: Message) {
        store.accept(ChatUDF.Action.ClientQuoteMessage(message))
    }

    fun stopQuoting() {
        store.accept(ChatUDF.Action.ClientStopQuoting)
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
            if (forward) ChatUDF.Action.InternalLoadMoreDownMessages
            else ChatUDF.Action.InternalLoadMoreUpMessages
        )
    }

    fun onQuotedMessageClick(chatItem: ChatItem) {
        chatItem.message.quotedMessage?.messageId?.let {
            store.accept(ChatUDF.Action.ClientScrollToMessage(it))
        }
    }

    fun atBottomOfChat(atBottom: Boolean) {
        store.accept(ChatUDF.Action.InternalAtBottom(atBottom))
    }

    fun scrollToBottom() {
        store.accept(ChatUDF.Action.ClientScrollToBottom)
    }

    fun emptyRetry() {
        store.accept(ChatUDF.Action.ClientEmptyRetry)
    }

    fun onFileClick(chatItem: ChatItem) {
        store.accept(ChatUDF.Action.ClientFileClick(chatItem.message))
    }
}
