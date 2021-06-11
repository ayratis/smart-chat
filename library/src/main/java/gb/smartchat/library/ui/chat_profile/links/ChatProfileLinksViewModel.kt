package gb.smartchat.library.ui.chat_profile.links

import android.util.Log
import androidx.lifecycle.ViewModel
import gb.smartchat.R
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ChatProfileLinksViewModel(
    private val chatId: Long,
    private val userId: String?,
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager,
    private val store: ChatProfileLinksUDF.Store
) : ViewModel() {

    companion object {
        private const val TAG = "ChatMediaViewModel"
        private const val RETRY_TAG = "retry tag"
    }

    private var fetchDisposable: Disposable? = null

    val listItems: Observable<List<ChatProfileLinkItem>> = store.hide().map { state ->
        when (state.pagingState) {
            ChatProfileLinksUDF.PagingState.EMPTY -> emptyList()
            ChatProfileLinksUDF.PagingState.EMPTY_PROGRESS -> listOf(ChatProfileLinkItem.Progress)
            ChatProfileLinksUDF.PagingState.EMPTY_ERROR -> listOf(
                ChatProfileLinkItem.Error(
                    message = resourceManager.getString(R.string.base_error_message),
                    action = resourceManager.getString(R.string.retry),
                    tag = RETRY_TAG
                )
            )
            ChatProfileLinksUDF.PagingState.DATA,
            ChatProfileLinksUDF.PagingState.NEW_PAGE_PROGRESS,
            ChatProfileLinksUDF.PagingState.FULL_DATA -> state.list.map {
                ChatProfileLinkItem.Data(it)
            }
        }
    }

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatProfileLinksUDF.SideEffect.ErrorEvent -> {
                    Log.d(TAG, "error event: ${sideEffect.error}")
                }
                is ChatProfileLinksUDF.SideEffect.LoadPage -> {
                    fetchLinks(sideEffect.pageCount)
                }
            }
        }
        store.accept(ChatProfileLinksUDF.Action.Refresh)
    }

    private fun fetchLinks(pageCount: Int) {
        fetchDisposable?.dispose()
        fetchDisposable = httpApi
            .getLinks(
                chatId = chatId,
                userId = userId,
                pageCount = pageCount,
                pageSize = 20
            )
            .map { it.result.links }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatProfileLinksUDF.Action.NewPage(pageCount, it)) },
                { store.accept(ChatProfileLinksUDF.Action.PageError(it)) }
            )
    }

    fun onLinkClick(link: String) {

    }

    fun onErrorActionClick(tag: String) {
        if (tag == RETRY_TAG) {
            store.accept(ChatProfileLinksUDF.Action.Refresh)
        }
    }

    fun loadMore() {
        if (store.currentState.pagingState != ChatProfileLinksUDF.PagingState.FULL_DATA) {
            store.accept(ChatProfileLinksUDF.Action.LoadMore)
        }
    }

    override fun onCleared() {
        fetchDisposable?.dispose()
    }
}
