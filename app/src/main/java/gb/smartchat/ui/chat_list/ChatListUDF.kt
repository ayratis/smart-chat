package gb.smartchat.ui.chat_list

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.entity.Chat
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

object ChatListUDF {

    const val DEFAULT_PAGE_SIZE = 20
    private const val TAG = "ChatListStateMachine"

    data class State(
        val chatList: List<Chat> = emptyList(),
        val pagingState: PagingState = PagingState.EMPTY,
        val pageCount: Int = 0
    )

    enum class PagingState {
        EMPTY,
        EMPTY_PROGRESS,
        EMPTY_ERROR,
        DATA,
        REFRESH,
        NEW_PAGE_PROGRESS,
        FULL_DATA
    }

    sealed class Action {
        object Refresh : Action()
        object LoadMore : Action()
        data class NewPage(val pageNumber: Int, val items: List<Chat>) : Action()
        data class PageError(val error: Throwable) : Action()
    }

    sealed class SideEffect {
        data class LoadPage(val pageCount: Int) : SideEffect()
        data class ErrorEvent(val error: Throwable) : SideEffect()
    }

    class Store : ObservableSource<State>, Consumer<Action>, Disposable {

        private val actions = PublishRelay.create<Action>()
        private val viewState = BehaviorRelay.createDefault(State())
        var sideEffectListener: (SideEffect) -> Unit = {}

        private var disposable: Disposable = actions
            .hide()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { action ->
                Log.d(TAG, "action: $action")
                val newState = reduce(viewState.value!!, action, sideEffectListener)
                Log.d(TAG, "state: $newState")
                viewState.accept(newState)
            }

        private fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
            when (action) {
                is Action.Refresh -> {
                    sideEffectListener(SideEffect.LoadPage(1))
                    val newPagingState = when (state.pagingState) {
                        PagingState.EMPTY,
                        PagingState.EMPTY_ERROR,
                        PagingState.EMPTY_PROGRESS -> PagingState.EMPTY_PROGRESS
                        PagingState.DATA,
                        PagingState.NEW_PAGE_PROGRESS,
                        PagingState.FULL_DATA,
                        PagingState.REFRESH -> PagingState.REFRESH
                    }
                    return state.copy(pagingState = newPagingState)
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
                        PagingState.EMPTY_PROGRESS, PagingState.REFRESH -> {
                            if (action.items.isEmpty()) {
                                state.copy(
                                    chatList = emptyList(),
                                    pagingState = PagingState.EMPTY
                                )
                            } else {
                                state.copy(
                                    chatList = action.items,
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
                                    chatList = state.chatList + action.items,
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
                        PagingState.REFRESH -> {
                            sideEffectListener(SideEffect.ErrorEvent(action.error))
                            state.copy(pagingState = PagingState.DATA)
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
        override fun accept(t: Action) {
            actions.accept(t)
        }

        override fun subscribe(observer: Observer<in State>) {
            viewState.hide().subscribe(observer)
        }

        override fun dispose() {
            disposable.dispose()
        }

        override fun isDisposed(): Boolean {
            return disposable.isDisposed
        }
    }
}