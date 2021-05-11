package gb.smartchat.ui.chat_profile.files

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
    }

    sealed class SideEffect {
        data class LoadPage(val pageCount: Int) : SideEffect()
        data class ErrorEvent(val error: Throwable) : SideEffect()
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
            }
        }
    }
}
