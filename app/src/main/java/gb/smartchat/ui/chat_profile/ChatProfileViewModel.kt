package gb.smartchat.ui.chat_profile

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.entity.Chat
import gb.smartchat.entity.request.AddRecipientsRequest
import gb.smartchat.entity.request.PinChatRequest
import gb.smartchat.publisher.ChatArchivePublisher
import gb.smartchat.publisher.LeaveChatPublisher
import gb.smartchat.utils.SingleEvent
import gb.smartchat.utils.toContact
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ChatProfileViewModel(
    private val httpApi: HttpApi,
    private val chat: Chat,
    private val userId: String,
    private val resourceManager: ResourceManager,
    private val leaveChatPublisher: LeaveChatPublisher,
    private val chatArchivePublisher: ChatArchivePublisher
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val exitToRootScreenCommand = BehaviorRelay.create<SingleEvent<Unit>>()
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()

    val exitToRootScreen: Observable<SingleEvent<Unit>> = exitToRootScreenCommand.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()

    fun leaveChat() {
        val me = chat.users.find { it.id == userId } ?: return
        httpApi
            .postLeaveChat(AddRecipientsRequest(chat.id, listOf(me.toContact())))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    leaveChatPublisher.accept(chat)
                    exitToRootScreenCommand.accept(SingleEvent(Unit))
                },
                {
                    val message = resourceManager.getString(R.string.leave_chat_error)
                    showDialogCommand.accept(SingleEvent(message))
                }
            )
            .also { compositeDisposable.add(it) }
    }

    fun archiveChat() {
        httpApi
            .postArchiveChat(PinChatRequest(chatId = chat.id))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chatArchivePublisher.accept(chat)
                    exitToRootScreenCommand.accept(SingleEvent(Unit))
                },
                {
                    val message = resourceManager.getString(R.string.archive_chat_error)
                    showDialogCommand.accept(SingleEvent(message))
                }
            )
            .also { compositeDisposable.add(it) }
    }

    override fun onCleared() {
        compositeDisposable.clear()
    }
}
