package gb.smartchat.ui.create_chat

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.entity.Group
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

object CreateChatUDF {

    private const val TAG = "CreateChatUDF"

    data class State(
        val contactsResponseState: ContactsResponseState = ContactsResponseState.Loading,
        val query: String? = null,
        val groupsToShow: List<Group> = emptyList()
    )

    sealed class ContactsResponseState {
        object Loading : ContactsResponseState()
        data class Data(val groups: List<Group>) : ContactsResponseState()
        data class Error(val error: Throwable) : ContactsResponseState()
    }

    sealed class Action {
        object Refresh : Action()
        data class LoadContactsSuccess(val groups: List<Group>) : Action()
        data class LoadContactsError(val error: Throwable) : Action()
        data class QueryTextChanged(val query: String?) : Action()
        data class QueryTextSubmit(val text: String?) : Action()
    }

    sealed class SideEffect {
        object LoadContacts : SideEffect()
    }

    class Store : ObservableSource<State>, Consumer<Action>, Disposable {

        private val actions = PublishRelay.create<Action>()
        private val viewState = BehaviorRelay.createDefault(State())
        var sideEffectListener: (SideEffect) -> Unit = {}

        private val disposable: Disposable = actions
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
                    sideEffectListener.invoke(SideEffect.LoadContacts)
                    val responseState = when (state.contactsResponseState) {
                        is ContactsResponseState.Data -> state.contactsResponseState
                        is ContactsResponseState.Error -> ContactsResponseState.Loading
                        is ContactsResponseState.Loading -> state.contactsResponseState
                    }
                    return state.copy(contactsResponseState = responseState)
                }
                is Action.LoadContactsSuccess -> {
                    val responseState = ContactsResponseState.Data(action.groups)
                    val groupsToShow = filterGroups(action.groups, state.query)
                    return state.copy(
                        contactsResponseState = responseState,
                        groupsToShow = groupsToShow
                    )
                }
                is Action.LoadContactsError -> {
                    val responseState = when (state.contactsResponseState) {
                        is ContactsResponseState.Data -> state.contactsResponseState
                        is ContactsResponseState.Error,
                        is ContactsResponseState.Loading -> ContactsResponseState.Error(action.error)
                    }
                    return state.copy(contactsResponseState = responseState)
                }
                is Action.QueryTextChanged -> {
                    return when (state.contactsResponseState) {
                        is ContactsResponseState.Data -> {
                            state.copy(
                                query = action.query,
                                groupsToShow = filterGroups(
                                    state.contactsResponseState.groups,
                                    action.query
                                )
                            )
                        }
                        is ContactsResponseState.Error,
                        is ContactsResponseState.Loading -> {
                            state.copy(query = action.query)
                        }
                    }
                }
                is Action.QueryTextSubmit -> {
                    return state
                }
            }
        }

        private fun filterGroups(groups: List<Group>, query: String?): List<Group> {
            if (query.isNullOrBlank()) return groups
            val list = mutableListOf<Group>()
            for (group in groups) {
                val contacts = group.contacts.filter {
                    it.name?.contains(query, ignoreCase = true) == true
                }
                if (contacts.isNotEmpty()) {
                    list += group.copy(contacts = contacts)
                }
            }
            return list
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
