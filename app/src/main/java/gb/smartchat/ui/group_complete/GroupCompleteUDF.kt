package gb.smartchat.ui.group_complete

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Contact
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

object GroupCompleteUDF {

    data class State(
        val photoUrl: String? = null,
        val groupName: String = "",
        val contacts: List<Contact>,
        val createGroupEnabled: Boolean = false,
        val loading: Boolean = false
    )

    sealed class Action {
        data class GroupNameChanged(val name: String) : Action()
        data class DeleteContact(val contact: Contact) : Action()
        object CreateGroup : Action()
        data class CreateGroupSuccess(val chat: Chat) : Action()
        data class CreateGroupError(val error: Throwable) : Action()
    }

    sealed class SideEffect {
        data class CreateGroup(
            val contacts: List<Contact>,
            val groupName: String,
            val photoUrl: String?
        ) : SideEffect()

        data class ShowCreateGroupError(val error: Throwable) : SideEffect()
        data class NavigateToChat(val chat: Chat) : SideEffect()
    }

    class Store(
        selectedContacts: List<Contact>
    ) : Observable<State>(), Consumer<Action>, Disposable {

        companion object {
            private const val TAG = "GroupCompleteUDF"
        }

        private val actions = PublishRelay.create<Action>()
        private val viewState = BehaviorRelay.createDefault(State(contacts = selectedContacts))
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
                is Action.CreateGroup -> {
                    sideEffectListener.invoke(
                        SideEffect.CreateGroup(
                            state.contacts,
                            state.groupName,
                            state.photoUrl
                        )
                    )
                    return state.copy(loading = true)
                }
                is Action.CreateGroupError -> {
                    sideEffectListener.invoke(SideEffect.ShowCreateGroupError(action.error))
                    return state.copy(loading = false)
                }
                is Action.CreateGroupSuccess -> {
                    sideEffectListener.invoke(SideEffect.NavigateToChat(action.chat))
                    return state.copy(loading = false)
                }
                is Action.DeleteContact -> {
                    val contacts = state.contacts.filter { it.id != action.contact.id }
                    return state.copy(
                        contacts = contacts,
                        createGroupEnabled = contacts.isNotEmpty() && state.groupName.isNotBlank()
                    )
                }
                is Action.GroupNameChanged -> {
                    return state.copy(
                        groupName = action.name,
                        createGroupEnabled = state.contacts.isNotEmpty() && action.name.isNotBlank()
                    )
                }
            }
        }

        override fun accept(t: Action) {
            actions.accept(t)
        }

        override fun dispose() {
            disposable.dispose()
        }

        override fun isDisposed(): Boolean {
            return disposable.isDisposed
        }

        override fun subscribeActual(observer: Observer<in State>) {
            viewState.hide().subscribe(observer)
        }
    }
}
