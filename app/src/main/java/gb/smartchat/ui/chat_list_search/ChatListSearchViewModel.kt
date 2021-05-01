package gb.smartchat.ui.chat_list_search

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.data.http.HttpApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ChatListSearchViewModel(
    private val httpApi: HttpApi,
    private val store: ChatListSearchUDF.Store
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private var searchDisposable: Disposable? = null
    private val query = BehaviorRelay.createDefault("")

    val viewState: Observable<ChatListSearchUDF.State> = store.hide()

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatListSearchUDF.SideEffect.ErrorEvent -> {
                    //todo?
                }
                is ChatListSearchUDF.SideEffect.LoadPage -> {
                    search(sideEffect.query, sideEffect.pageCount)
                }
            }
        }

        query
            .debounce(500, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .subscribe { store.accept(ChatListSearchUDF.Action.SubmitQuery(it)) }
            .also { compositeDisposable.add(it) }
    }

    private fun search(query: String, pageCount: Int) {
        searchDisposable?.dispose()
        httpApi
            .getSearchChats(query, pageCount, 20)
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatListSearchUDF.Action.NewPage(pageCount, it.chats)) },
                { store.accept(ChatListSearchUDF.Action.PageError(it)) }
            )
            .also {
                searchDisposable = it
                compositeDisposable.add(it)
            }
    }

    fun onTextChanged(text: String) {
        query.accept(text)
    }

    fun submitQuery() {
        store.accept(ChatListSearchUDF.Action.SubmitQuery(query.value!!))
    }

    fun loadMore() {
        store.accept(ChatListSearchUDF.Action.LoadMore)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
