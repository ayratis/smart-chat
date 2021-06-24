package gb.smartchat.library.ui.create_chat

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.entity.Group
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
        val groupsToShow: List<Group> = emptyList(),
        val blockingProgress: Boolean = false,
        val selectedContacts: List<Contact> = emptyList()
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
        data class OnContactClick(val contact: Contact) : Action()
        data class CreateChatSuccess(val chat: Chat) : Action()
        data class CreateChatError(val error: Throwable) : Action()
        object DoneClick : Action()
        data class DeleteContact(val contact: Contact) : Action()
        object AddMembersSuccess : Action()
        data class AddMembersError(val error: Throwable) : Action()
    }

    sealed class SideEffect {
        object LoadContacts : SideEffect()
        data class ShowErrorMessage(val error: Throwable) : SideEffect()
        data class NavigateToChat(val chat: Chat) : SideEffect()
        data class NavigateToGroupComplete(val selectedContacts: List<Contact>) : SideEffect()
        data class AddMembersToChat(val selectedContacts: List<Contact>) : SideEffect()
        object Exit : SideEffect()
    }

    class Store(
        private val mode: CreateChatMode
    ) : ObservableSource<State>, Consumer<Action>, Disposable {

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
                is Action.OnContactClick -> {
                    val selectedContacts =
                        if (state.selectedContacts.contains(action.contact)) {
                            state.selectedContacts.filter { it.id != action.contact.id }
                        } else {
                            state.selectedContacts + action.contact
                        }
                    return state.copy(selectedContacts = selectedContacts)
                }
                is Action.CreateChatError -> {
                    sideEffectListener.invoke(SideEffect.ShowErrorMessage(action.error))
                    return state.copy(blockingProgress = false)
                }
                is Action.CreateChatSuccess -> {
                    sideEffectListener.invoke(SideEffect.NavigateToChat(action.chat))
                    return state.copy(blockingProgress = false)
                }
                is Action.DoneClick -> {
                    when (mode) {
                        CreateChatMode.CREATE_GROUP -> {
                            if (state.selectedContacts.isNotEmpty()) {
                                sideEffectListener.invoke(
                                    SideEffect.NavigateToGroupComplete(state.selectedContacts)
                                )
                            }
                            return state
                        }
                        CreateChatMode.ADD_MEMBERS -> {
                            if (state.selectedContacts.isNotEmpty()) {
                                sideEffectListener.invoke(
                                    SideEffect.AddMembersToChat(state.selectedContacts)
                                )
                            }
                            return state.copy(blockingProgress = true)
                        }
                    }

                }
                is Action.DeleteContact -> {
                    return state.copy(
                        selectedContacts = state.selectedContacts.filter {
                            it.id != action.contact.id
                        }
                    )
                }
                is Action.AddMembersSuccess -> {
                    sideEffectListener.invoke(SideEffect.Exit)
                    return state.copy(blockingProgress = false)
                }
                is Action.AddMembersError -> {
                    sideEffectListener.invoke(SideEffect.ShowErrorMessage(action.error))
                    return state.copy(blockingProgress = false)
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
