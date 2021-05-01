package gb.smartchat.ui.chat_list

import android.util.Log
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.data.socket.SocketApi
import gb.smartchat.data.socket.SocketEvent
import gb.smartchat.entity.StoreInfo
import gb.smartchat.publisher.ChatCreatedPublisher
import gb.smartchat.publisher.ChatUnreadMessageCountPublisher
import gb.smartchat.publisher.MessageReadInternalPublisher
import gb.smartchat.utils.SingleEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ChatListViewModel(
    private val store: ChatListUDF.Store,
    private val httpApi: HttpApi,
    private val socketApi: SocketApi,
    private val resourceManager: ResourceManager,
    chatCreatedPublisher: ChatCreatedPublisher,
    messageReadInternalPublisher: MessageReadInternalPublisher,
    chatUnreadMessageCountPublisher: ChatUnreadMessageCountPublisher
) : ViewModel() {

    companion object {
        private const val TAG = "ChatListViewModel"
    }

    private val compositeDisposable = CompositeDisposable()
    private val showProfileErrorCommand = BehaviorRelay.create<SingleEvent<String>>()
    private val navToCreateChatCommand = BehaviorRelay.create<SingleEvent<StoreInfo>>()

    val viewState: Observable<ChatListUDF.State> = Observable.wrap(store)
    val showProfileErrorDialog: Observable<SingleEvent<String>> = showProfileErrorCommand.hide()
    val navToCreateChat: Observable<SingleEvent<StoreInfo>> = navToCreateChatCommand.hide()

    override fun onCleared() {
        compositeDisposable.dispose()
    }

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
    }

    private fun setupStateMachine() {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatListUDF.SideEffect.LoadUserProfile -> {
                    fetchUserProfile()
                }
                is ChatListUDF.SideEffect.ShowProfileLoadError -> {
                    val message = resourceManager.getString(R.string.profile_error)
                    showProfileErrorCommand.accept(SingleEvent(message))
                }
                is ChatListUDF.SideEffect.ErrorEvent -> {
                    Log.d(TAG, "errorEvent", sideEffect.error)
                }
                is ChatListUDF.SideEffect.LoadPage -> {
                    fetchPage(sideEffect.pageCount)
                }
                is ChatListUDF.SideEffect.NavToCreateChat -> {
                    navToCreateChatCommand.accept(SingleEvent(sideEffect.storeInfo))
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
                }
            }
            .also { compositeDisposable.add(it) }
    }

    private fun fetchUserProfile() {
        httpApi
            .getUserProfile()
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatListUDF.Action.ProfileSuccess(it)) },
                { store.accept(ChatListUDF.Action.ProfileError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun fetchPage(pageCount: Int) {
        httpApi
            .getChatList(
                pageCount = pageCount,
                pageSize = ChatListUDF.DEFAULT_PAGE_SIZE,
                fromArchive = null
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

    fun onCreateChatClick() {
        store.accept(ChatListUDF.Action.CreateChat)
    }

    fun loadMore() {
        store.accept(ChatListUDF.Action.LoadMore)
    }
}
