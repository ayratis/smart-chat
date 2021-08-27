package gb.smartchat.library.ui._global

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer


abstract class BaseStore<State: Any, Action: Any, SideEffect: Any>(
    initialState: State,
): Observable<State>(), Consumer<Action>, Disposable {

    private val tag by lazy {
        this::class.java.canonicalName
    }

    private val actions = PublishRelay.create<Action>()
    private val viewState = BehaviorRelay.createDefault(initialState)
    var sideEffectListener: (SideEffect) -> Unit = {}
    val currentState: State
        get() = viewState.value!!

    private val disposable: Disposable = actions
        .hide()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { action ->
            Log.d(tag, "action: $action")
            val newState = reduce(viewState.value!!, action) {
                Log.d(tag, "sideEffect: $it")
                sideEffectListener.invoke(it)
            }
            Log.d(tag, "state: $newState")
            viewState.accept(newState)
        }

    abstract fun reduce(
        state: State,
        action: Action,
        sideEffectListener: (SideEffect) -> Unit
    ): State

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
