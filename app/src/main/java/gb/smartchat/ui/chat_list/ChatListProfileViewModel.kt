package gb.smartchat.ui.chat_list

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.entity.StoreInfo
import gb.smartchat.utils.SingleEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ChatListProfileViewModel(
    private val httpApi: HttpApi,
    private val store: ChatListProfileUDF.Store,
    private val resourceManager: ResourceManager,
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val showProfileErrorCommand = BehaviorRelay.create<SingleEvent<String>>()
    private val navToCreateChatCommand = BehaviorRelay.create<SingleEvent<StoreInfo>>()

    val viewState: Observable<ChatListProfileUDF.State> = store.hide()
    val showProfileErrorDialog: Observable<SingleEvent<String>> = showProfileErrorCommand.hide()
    val navToCreateChat: Observable<SingleEvent<StoreInfo>> = navToCreateChatCommand.hide()

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is ChatListProfileUDF.SideEffect.LoadUserProfile -> {
                    fetchUserProfile()
                }
                is ChatListProfileUDF.SideEffect.NavToCreateChat -> {
                    navToCreateChatCommand.accept(SingleEvent(sideEffect.storeInfo))
                }
                is ChatListProfileUDF.SideEffect.ShowProfileLoadError -> {
                    val message = resourceManager.getString(R.string.profile_error)
                    showProfileErrorCommand.accept(SingleEvent(message))
                }
            }
        }
        store.accept(ChatListProfileUDF.Action.Refresh)
    }

    private fun fetchUserProfile() {
        httpApi
            .getUserProfile()
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(ChatListProfileUDF.Action.ProfileSuccess(it)) },
                { store.accept(ChatListProfileUDF.Action.ProfileError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    fun onCreateChatClick() {
        store.accept(ChatListProfileUDF.Action.CreateChat)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
