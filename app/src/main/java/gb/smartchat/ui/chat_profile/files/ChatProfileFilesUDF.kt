package gb.smartchat.ui.chat_profile.files

import android.net.Uri
import gb.smartchat.data.download.DownloadStatus
import gb.smartchat.entity.File
import gb.smartchat.ui._global.BaseStore

object ChatProfileFilesUDF {

    data class State(
        val list: List<File> = emptyList(),
        val pagingState: PagingState = PagingState.EMPTY,
        val pageCount: Int = 0
    )

    enum class PagingState {
        EMPTY,
        EMPTY_PROGRESS,
        EMPTY_ERROR,
        DATA,
        NEW_PAGE_PROGRESS,
        FULL_DATA
    }

    sealed class Action {
        object Refresh : Action()
        object LoadMore : Action()
        data class NewPage(val pageNumber: Int, val items: List<File>) : Action()
        data class PageError(val error: Throwable) : Action()
        data class FileClick(val file: File) : Action()
        data class UpdateFileDownloadStatus(
            val file: File,
            val newDownloadStatus: DownloadStatus
        ) : Action()
    }

    sealed class SideEffect {
        data class LoadPage(val pageCount: Int) : SideEffect()
        data class ErrorEvent(val error: Throwable) : SideEffect()
        data class DownloadFile(val file: File) : SideEffect()
        data class CancelDownloadFile(val file: File) : SideEffect()
        data class OpenFile(val contentUri: Uri) : SideEffect()
    }

    class Store : BaseStore<State, Action, SideEffect>(State()) {

        override fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
            when (action) {
                is Action.Refresh -> {
                    sideEffectListener(SideEffect.LoadPage(1))
                    return state.copy(pagingState = PagingState.EMPTY_PROGRESS)
                }
                is Action.LoadMore -> {
                    return when (state.pagingState) {
                        PagingState.DATA -> {
                            sideEffectListener(SideEffect.LoadPage(state.pageCount + 1))
                            state.copy(pagingState = PagingState.NEW_PAGE_PROGRESS)
                        }
                        else -> state
                    }
                }
                is Action.NewPage -> {
                    return when (state.pagingState) {
                        PagingState.EMPTY_PROGRESS -> {
                            if (action.items.isEmpty()) {
                                state.copy(
                                    list = emptyList(),
                                    pagingState = PagingState.EMPTY
                                )
                            } else {
                                state.copy(
                                    list = action.items,
                                    pagingState = PagingState.DATA,
                                    pageCount = 1
                                )
                            }
                        }
                        PagingState.NEW_PAGE_PROGRESS -> {
                            if (action.items.isEmpty()) {
                                state.copy(pagingState = PagingState.FULL_DATA)
                            } else {
                                state.copy(
                                    list = state.list + action.items,
                                    pagingState = PagingState.DATA,
                                    pageCount = state.pageCount + 1
                                )
                            }
                        }
                        else -> state
                    }
                }
                is Action.PageError -> {
                    return when (state.pagingState) {
                        PagingState.EMPTY_PROGRESS -> {
                            state.copy(pagingState = PagingState.EMPTY_ERROR)
                        }
                        PagingState.NEW_PAGE_PROGRESS -> {
                            sideEffectListener(SideEffect.ErrorEvent(action.error))
                            state.copy(pagingState = PagingState.DATA)
                        }
                        else -> state
                    }
                }
                is Action.FileClick -> {
                    return when (val status = action.file.downloadStatus) {
                        is DownloadStatus.Empty -> {
                            sideEffectListener(SideEffect.DownloadFile(action.file))
                            state
                        }
                        is DownloadStatus.Downloading -> {
                            sideEffectListener(SideEffect.CancelDownloadFile(action.file))
                            state
                        }
                        is DownloadStatus.Success -> {
                            sideEffectListener(SideEffect.OpenFile(status.contentUri))
                            state
                        }
                    }
                }
                is Action.UpdateFileDownloadStatus -> {
                    val file = action.file.copy(downloadStatus = action.newDownloadStatus)
                    val index = state.list.indexOfFirst { it.url == file.url }
                    val list = state.list.toMutableList()
                    if (index >= 0) {
                        list[index] = file
                    }
                    if (action.newDownloadStatus is DownloadStatus.Success) { //автоматическое открытие файла
                        sideEffectListener(SideEffect.OpenFile(action.newDownloadStatus.contentUri))
                    }
                    return state.copy(list = list)
                }
            }
        }
    }
}
