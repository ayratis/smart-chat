package gb.smartchat.ui.chat_profile.media

import android.util.Log
import androidx.lifecycle.ViewModel
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.entity.File
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ChatMediaViewModel(
    private val chatId: Long,
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager,
    private val store: ChatMediaUDF.Store
) : ViewModel() {

    companion object {
        private const val TAG = "ChatMediaViewModel"
        private const val RETRY_TAG = "retry tag"
    }

    private var fetchDisposable: Disposable? = null

    val listItems: Observable<List<MediaItem>> = store.hide().map { state ->
        when (state.pagingState) {
            ChatMediaUDF.PagingState.EMPTY -> emptyList()
            ChatMediaUDF.PagingState.EMPTY_PROGRESS -> listOf(MediaItem.Progress)
            ChatMediaUDF.PagingState.EMPTY_ERROR -> listOf(
                MediaItem.Error(
                    message = resourceManager.getString(R.string.base_error_message),
                    action = resourceManager.getString(R.string.retry),
                    tag = RETRY_TAG
                )
            )
            ChatMediaUDF.PagingState.DATA -> state.list.map { MediaItem.Data(it) }
            ChatMediaUDF.PagingState.NEW_PAGE_PROGRESS -> state.list.map { MediaItem.Data(it) }
            ChatMediaUDF.PagingState.FULL_DATA -> state.list.map { MediaItem.Data(it) }
        }
    }

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatMediaUDF.SideEffect.ErrorEvent -> {
                    Log.d(TAG, "error event: ${sideEffect.error}")
                }
                is ChatMediaUDF.SideEffect.LoadPage -> {
                    fetchFiles(sideEffect.pageCount)
                }
            }
        }
        store.accept(ChatMediaUDF.Action.Refresh)
    }

    private fun fetchFiles(pageCount: Int) {
        fetchDisposable?.dispose()
        fetchDisposable = httpApi
            .getFiles(
                chatId = chatId,
                type = "media",
                pageCount = pageCount,
                pageSize = 20
            )
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatMediaUDF.Action.NewPage(pageCount, it)) },
                { store.accept(ChatMediaUDF.Action.PageError(it)) }
            )
    }

    fun onFileClick(file: File) {

    }

    fun onErrorActionClick(tag: String) {
        if (tag == RETRY_TAG) {
            store.accept(ChatMediaUDF.Action.Refresh)
        }
    }

    fun loadMore() {
        if (store.currentState.pagingState != ChatMediaUDF.PagingState.FULL_DATA) {
            store.accept(ChatMediaUDF.Action.LoadMore)
        }
    }

    override fun onCleared() {
        fetchDisposable?.dispose()
    }
}
