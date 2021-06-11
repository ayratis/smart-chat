package gb.smartchat.library.ui.chat_list

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.humanMessage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class FavoriteMessagesViewModel(
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val navToFavoriteChatCommand = BehaviorRelay.create<SingleEvent<Chat>>()
    private val showMessageDialogCommand = BehaviorRelay.create<SingleEvent<String>>()
    private val fullScreenProgressBehavior = BehaviorRelay.createDefault(false)

    val navToFavoriteChat: Observable<SingleEvent<Chat>> = navToFavoriteChatCommand.hide()
    val showMessageDialog: Observable<SingleEvent<String>> = showMessageDialogCommand.hide()
    val fullScreenProgress: Observable<Boolean> = fullScreenProgressBehavior.hide()

    private fun fetchFavoriteChat() {
        httpApi
            .getFavoriteChat()
            .map { it.result.chat }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { fullScreenProgressBehavior.accept(true) }
            .doFinally { fullScreenProgressBehavior.accept(false) }
            .subscribe(
                {
                    navToFavoriteChatCommand.accept(SingleEvent(it))
                },
                {
                    val message = it.humanMessage(resourceManager)
                    showMessageDialogCommand.accept(SingleEvent(message))
                }
            )
            .also {
                compositeDisposable.add(it)
            }
    }

    fun onFavoriteMessagesClick() {
        fetchFavoriteChat()
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
