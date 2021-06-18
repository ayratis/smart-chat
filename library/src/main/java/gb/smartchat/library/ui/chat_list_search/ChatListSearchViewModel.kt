package gb.smartchat.library.ui.chat_list_search

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.Contact
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ChatListSearchViewModel(
    private val httpApi: HttpApi,
    private val store: ChatListSearchUDF.Store,
    private val resourceManager: ResourceManager
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private var searchDisposable: Disposable? = null
    private val query = BehaviorRelay.createDefault("")

    val fullData: Observable<Boolean> = store.hide()
        .map { it.pagingState == ChatListSearchUDF.PagingState.FULL_DATA }
        .distinctUntilChanged()

    val items: Observable<List<SearchItem>> = store.hide()
        .map { state ->
            val items = mutableListOf<SearchItem>()
            if (state.chats.isNotEmpty()) {
                items += SearchItem.Header(resourceManager.getString(R.string.chats))
            }
            items += state.chats.map { SearchItem.Chat(it) }
            if (state.contacts.isNotEmpty()) {
                items += SearchItem.Header(resourceManager.getString(R.string.contacts))
            }
            items += state.contacts.map { SearchItem.Contact(it) }
            items
        }

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
            .getSearchChats(query, pageCount, 20, true)
            .map {
                val chats = it.result.chats
                val contacts = mutableListOf<Contact>()
                for (group in it.result.contacts) {
                    contacts += group.contacts
                }
                chats to contacts
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (chats, contacts) ->
                    store.accept(ChatListSearchUDF.Action.NewPage(pageCount, chats, contacts))
                },
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
