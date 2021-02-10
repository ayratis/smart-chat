package gb.smartchat.ui.chat

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import gb.smartchat.data.socket.SocketApi
import gb.smartchat.data.socket.SocketEvent
import gb.smartchat.di.InstanceFactory
import gb.smartchat.entity.Message
import gb.smartchat.entity.request.MessageReadRequest
import gb.smartchat.ui.chat.state_machine.Action
import gb.smartchat.ui.chat.state_machine.SideEffect
import gb.smartchat.ui.chat.state_machine.Store
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class ChatViewModel(
    private val store: Store = Store("77f21ecc-0d4a-4f85-9173-55acf327f007"),
    private val userId: String = "77f21ecc-0d4a-4f85-9173-55acf327f007",
    private val chatId: Long = 1,
    private val socketApi: SocketApi = InstanceFactory.createSocketApi()
) : ViewModel() {

    companion object {
        const val TAG = "ChatViewModel"
    }

    private val compositeDisposable = CompositeDisposable()
    private val typingTimersDisposableMap = HashMap<String, Disposable>()
    val chatList = MutableLiveData<List<ChatItem>>(emptyList())

    init {
        setupStateMachine()
        observeSocketEvents()
    }

    fun onStart() {
        socketApi.connect()
    }

    fun onSendClick(text: String) {
        store.accept(Action.ClientSendMessage(text))
    }

    fun onChatItemBind(chatItem: ChatItem) {
        if (chatItem is ChatItem.Incoming/*|| chatItem is ChatItem.System*/) {
            if (!chatItem.message.readedIds.contains(userId)) {
                val requestBody = MessageReadRequest(
                    messageIds = listOf(chatItem.message.id),
                    chatId = chatId,
                    senderId = userId,
                )
                val d = socketApi.readMessage(requestBody)
                    .subscribe(
                        { /*do nothing*/ },
                        { e ->
                            Log.e(TAG, "messageRead", e)
                        }
                    )
                compositeDisposable.add(d)
            }
        }
    }

    private fun setupStateMachine() {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is SideEffect.SendMessage -> sendMessage(sideEffect.message)
                is SideEffect.TypingTimer -> startTypingTimer(sideEffect.senderId)
                is SideEffect.EditMessage -> editMessage(sideEffect.message, sideEffect.newText)
            }
        }
        compositeDisposable.add(
            Observable.wrap(store)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    Log.d(TAG, "viewState: $state")
                    chatList.value = state.chatItems
                }
        )
    }

    private fun observeSocketEvents() {
        val d = socketApi.observeEvents()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when (event) {
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
        val messageEditRequest = message.toMessageEditRequestBody() ?: return
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

    private fun startTypingTimer(senderId: String) {
        typingTimersDisposableMap[senderId]?.dispose()
        typingTimersDisposableMap[senderId] = Completable
            .timer(2, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                store.accept(Action.InternalTypingTimeIsUp(senderId))
            }
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

}