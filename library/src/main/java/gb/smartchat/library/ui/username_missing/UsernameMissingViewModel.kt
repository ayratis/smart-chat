package gb.smartchat.library.ui.username_missing

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.request.UsernameRequest
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.humanMessage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class UsernameMissingViewModel(
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private val showErrorDialogCommand = BehaviorRelay.create<SingleEvent<String>>()
    private val navToChatsCommand = BehaviorRelay.create<SingleEvent<Unit>>()
    private val progressState = BehaviorRelay.createDefault(false)

    val showErrorDialog: Observable<SingleEvent<String>> = showErrorDialogCommand.hide()
    val navToChats: Observable<SingleEvent<Unit>> = navToChatsCommand.hide()
    val progress: Observable<Boolean> = progressState.hide()

    fun onContinueClicked(name: String) {
        if (progressState.value == true) return

        httpApi
            .postUsername(UsernameRequest(name))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                progressState.accept(true)
            }
            .doFinally {
                progressState.accept(false)
            }
            .subscribe(
                {
                    navToChatsCommand.accept(SingleEvent(Unit))
                },
                { error ->
                    val message = error.humanMessage(resourceManager)
                    showErrorDialogCommand.accept(SingleEvent(message))
                }
            )
            .also(compositeDisposable::add)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
