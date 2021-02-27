package gb.smartchat.ui.chat_list

import android.util.Log
import androidx.lifecycle.ViewModel
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.socket.SocketApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ChatListViewModel(
    private val store: ChatListUDF.Store,
    private val httpApi: HttpApi,
    private val socketApi: SocketApi
) : ViewModel() {

    companion object {
        private const val TAG = "ChatListViewModel"
    }

    private val compositeDisposable = CompositeDisposable()

    val viewState: Observable<ChatListUDF.State> = Observable.wrap(store)

    override fun onCleared() {
        compositeDisposable.dispose()
    }

    init {
        setupStateMachine()
        store.accept(ChatListUDF.Action.Refresh)
    }

    private fun setupStateMachine() {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatListUDF.SideEffect.ErrorEvent ->
                    Log.d(TAG, "errorEvent", sideEffect.error)
                is ChatListUDF.SideEffect.LoadPage -> fetchPage(sideEffect.pageCount)
            }
        }
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
}
