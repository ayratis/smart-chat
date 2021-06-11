package gb.smartchat.library.ui.chat_list_search

import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.ui._global.BaseStore

object ChatListSearchUDF {

    data class State(
        val chats: List<Chat> = emptyList(),
        val contacts: List<Contact> = emptyList(),
        val pagingState: PagingState = PagingState.EMPTY,
        val query: String = "",
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
        data class SubmitQuery(val text: String) : Action()
        object LoadMore : Action()
        data class NewPage(val pageNumber: Int, val chats: List<Chat>, val contacts: List<Contact>) : Action()
        data class PageError(val error: Throwable) : Action()
    }

    sealed class SideEffect {
        data class LoadPage(val query: String, val pageCount: Int) : SideEffect()
        data class ErrorEvent(val error: Throwable) : SideEffect()
    }

    class Store : BaseStore<State, Action, SideEffect>(State()) {

        override fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
            when (action) {
                is Action.SubmitQuery -> {
                    if (action.text.isBlank()) return state.copy(
                        chats = emptyList(),
                        contacts = emptyList(),
                        pageCount = 0,
                        pagingState = PagingState.EMPTY,
                        query = ""
                    )
                    if (action.text == state.query) return state
                    sideEffectListener(SideEffect.LoadPage(action.text, 1))
                    return state.copy(pagingState = PagingState.EMPTY_PROGRESS, query = action.text)
                }
                is Action.LoadMore -> {
                    return when (state.pagingState) {
                        PagingState.DATA -> {
                            sideEffectListener(
                                SideEffect.LoadPage(
                                    state.query,
                                    state.pageCount + 1
                                )
                            )
                            state.copy(pagingState = PagingState.NEW_PAGE_PROGRESS)
                        }
                        else -> state
                    }
                }
                is Action.NewPage -> {
                    return when (state.pagingState) {
                        PagingState.EMPTY_PROGRESS -> {
                            if (action.chats.isEmpty() && action.contacts.isEmpty()) {
                                state.copy(
                                    chats = emptyList(),
                                    contacts = emptyList(),
                                    pagingState = PagingState.EMPTY
                                )
                            } else {
                                state.copy(
                                    chats = action.chats,
                                    contacts = action.contacts,
                                    pagingState = PagingState.DATA,
                                    pageCount = 1
                                )
                            }
                        }
                        PagingState.NEW_PAGE_PROGRESS -> {
                            if (action.chats.size < 20) {
                                state.copy(pagingState = PagingState.FULL_DATA)
                            } else {
                                state.copy(
                                    chats = state.chats + action.chats,
                                    contacts = state.contacts + action.contacts,
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
