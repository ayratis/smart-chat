package gb.smartchat.library.ui.service_chat_preparing

import android.util.Log
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.humanMessage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ServiceChatPreparingViewModel(
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager,
    private val storeInfo: StoreInfo,
) : ViewModel() {

    private var fetchServiceChatDisposable: Disposable? = null

    private val navToServiceChatCommand = BehaviorRelay.create<SingleEvent<Chat>>()
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()

    val navToServiceChat: Observable<SingleEvent<Chat>> = navToServiceChatCommand.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()

    init {
        fetchServiceChat()
    }

    private fun fetchServiceChat() {
        httpApi
            .getServiceChat(storeInfo.storeId, storeInfo.partnerCode)
            .map { it.result.chat }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { serviceChat ->
                    Log.d("ServiceChatPreparingVM", "fetchServiceChat: $serviceChat")
                    navToServiceChatCommand.accept(SingleEvent(serviceChat))
                },
                { error ->
                    val message = error.humanMessage(resourceManager)
                    showDialogCommand.accept(SingleEvent(message))
                }
            )
            .also { fetchServiceChatDisposable = it }
    }

    override fun onCleared() {
        fetchServiceChatDisposable?.dispose()
    }
}