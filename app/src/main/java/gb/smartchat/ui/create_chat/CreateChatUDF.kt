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

    sealed class State {
        object Empty : State()
        object Loading : State()
        data class Data(val groups: List<Group>) : State()
        data class Error(val error: Throwable) : State()
    }

    sealed class Action {
        object Refresh : Action()
        data class LoadContactsSuccess(val groups: List<Group>) : Action()
        data class LoadContactsError(val error: Throwable) : Action()
    }

    sealed class SideEffect {
        object LoadContacts : SideEffect()
    }

    class Store : ObservableSource<State>, Consumer<Action>, Disposable {

        private val actions = PublishRelay.create<Action>()
        private val viewState = BehaviorRelay.createDefault<State>(State.Empty)
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
                    return State.Loading
                }
                is Action.LoadContactsSuccess -> {
                    return State.Data(action.groups)
                }
                is Action.LoadContactsError -> {
                    return when (state) {
                        is State.Data -> state
                        is State.Empty,
                        is State.Error,
                        is State.Loading -> State.Error(action.error)
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
