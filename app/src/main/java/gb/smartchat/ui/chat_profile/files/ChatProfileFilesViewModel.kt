package gb.smartchat.ui.chat_profile.files

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

class ChatProfileFilesViewModel(
    private val chatId: Long,
    private val isMedia: Boolean,
    private val userId: String?,
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager,
    private val store: ChatProfileFilesUDF.Store,
) : ViewModel() {

    companion object {
        private const val TAG = "ChatMediaViewModel"
        private const val RETRY_TAG = "retry tag"
    }

    private var fetchDisposable: Disposable? = null

    val listItems: Observable<List<ChatProfileFileItem>> = store.hide().map { state ->
        when (state.pagingState) {
            ChatProfileFilesUDF.PagingState.EMPTY -> emptyList()
            ChatProfileFilesUDF.PagingState.EMPTY_PROGRESS -> listOf(ChatProfileFileItem.Progress)
            ChatProfileFilesUDF.PagingState.EMPTY_ERROR -> listOf(
                ChatProfileFileItem.Error(
                    message = resourceManager.getString(R.string.base_error_message),
                    action = resourceManager.getString(R.string.retry),
                    tag = RETRY_TAG
                )
            )
            ChatProfileFilesUDF.PagingState.DATA -> state.list.map {
                if (isMedia) ChatProfileFileItem.Media(it) else ChatProfileFileItem.Doc(it)
            }
            ChatProfileFilesUDF.PagingState.NEW_PAGE_PROGRESS -> state.list.map {
                if (isMedia) ChatProfileFileItem.Media(it) else ChatProfileFileItem.Doc(it)
            }
            ChatProfileFilesUDF.PagingState.FULL_DATA -> state.list.map {
                if (isMedia) ChatProfileFileItem.Media(it) else ChatProfileFileItem.Doc(it)
            }
        }
    }

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatProfileFilesUDF.SideEffect.ErrorEvent -> {
                    Log.d(TAG, "error event: ${sideEffect.error}")
                }
                is ChatProfileFilesUDF.SideEffect.LoadPage -> {
                    fetchFiles(sideEffect.pageCount)
                }
            }
        }
        store.accept(ChatProfileFilesUDF.Action.Refresh)
    }

    private fun fetchFiles(pageCount: Int) {
        fetchDisposable?.dispose()
        fetchDisposable = httpApi
            .getFiles(
                chatId = chatId,
                type = if (isMedia) "media" else "regular",
                userId = userId,
                pageCount = pageCount,
                pageSize = 20
            )
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatProfileFilesUDF.Action.NewPage(pageCount, it)) },
                { store.accept(ChatProfileFilesUDF.Action.PageError(it)) }
            )
    }

    fun onFileClick(file: File) {

    }

    fun onErrorActionClick(tag: String) {
        if (tag == RETRY_TAG) {
            store.accept(ChatProfileFilesUDF.Action.Refresh)
        }
    }

    fun loadMore() {
        if (store.currentState.pagingState != ChatProfileFilesUDF.PagingState.FULL_DATA) {
            store.accept(ChatProfileFilesUDF.Action.LoadMore)
        }
    }

    override fun onCleared() {
        fetchDisposable?.dispose()
    }
}
