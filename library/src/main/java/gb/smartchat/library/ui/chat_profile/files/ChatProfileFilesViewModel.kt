package gb.smartchat.library.ui.chat_profile.files

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.library.data.download.DownloadStatus
import gb.smartchat.library.data.download.FileDownloadHelper
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.File
import gb.smartchat.library.utils.SingleEvent
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
    private val downloadHelper: FileDownloadHelper
) : ViewModel() {

    companion object {
        private const val TAG = "ChatMediaViewModel"
        private const val RETRY_TAG = "retry tag"
    }

    private var fetchDisposable: Disposable? = null
    private val downloadStatusDisposableMap = HashMap<String, Disposable>()
    private val openFileCommand = BehaviorRelay.create<SingleEvent<Uri>>()

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

    val openFile: Observable<SingleEvent<Uri>> = openFileCommand.hide()

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatProfileFilesUDF.SideEffect.ErrorEvent -> {
                    Log.d(TAG, "error event: ${sideEffect.error}")
                }
                is ChatProfileFilesUDF.SideEffect.LoadPage -> {
                    fetchFiles(sideEffect.pageCount)
                }
                is ChatProfileFilesUDF.SideEffect.CancelDownloadFile -> {
                    cancelDownloadMessageFile(sideEffect.file)
                }
                is ChatProfileFilesUDF.SideEffect.DownloadFile -> {
                    downloadFile(sideEffect.file)
                }
                is ChatProfileFilesUDF.SideEffect.OpenFile -> {
                    openFileCommand.accept(SingleEvent(sideEffect.contentUri))
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
            .map { it.result.composeWithDownloadStatus() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatProfileFilesUDF.Action.NewPage(pageCount, it)) },
                { store.accept(ChatProfileFilesUDF.Action.PageError(it)) }
            )
    }

    private fun downloadFile(file: File) {
        file.url ?: return
        val d = downloadStatusDisposableMap[file.url]
        if (d?.isDisposed == false) {
            d.dispose()
        }
        downloadHelper.download(file.url)
            .subscribe { downloadStatus ->
                store.accept(
                    ChatProfileFilesUDF.Action.UpdateFileDownloadStatus(
                        file,
                        downloadStatus
                    )
                )
            }.also {
                downloadStatusDisposableMap[file.url] = it
            }
    }

    private fun cancelDownloadMessageFile(file: File) {
        file.url?.let { downloadHelper.cancelDownload(it) }
    }

    private fun List<File>.composeWithDownloadStatus(): List<File> {
        return this.map { file ->
            if (file.url != null) {
                val downloadStatus = downloadHelper.getDownloadStatus(file.url)
                file.copy(downloadStatus = downloadStatus)
            } else {
                file.copy(downloadStatus = DownloadStatus.Empty)
            }
        }
    }

    fun onFileClick(file: File) {
        store.accept(ChatProfileFilesUDF.Action.FileClick(file))
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

        downloadStatusDisposableMap.values.forEach { d ->
            if (!d.isDisposed) {
                d.dispose()
            }
        }
    }
}
