package gb.smartchat.ui.group_complete

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Contact
import gb.smartchat.entity.StoreInfo
import gb.smartchat.publisher.ChatCreatedPublisher
import gb.smartchat.publisher.ContactDeletePublisher
import gb.smartchat.utils.SingleEvent
import gb.smartchat.utils.humanMessage
import gb.smartchat.utils.toCreateChatRequest
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class GroupCompleteViewModel(
    private val storeInfo: StoreInfo,
    private val store: GroupCompleteUDF.Store,
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager,
    private val chatCreatedPublisher: ChatCreatedPublisher,
    private val contactDeletePublisher: ContactDeletePublisher
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private val navToChatCommand = BehaviorRelay.create<SingleEvent<Chat>>()
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()

    val contacts: Observable<List<Contact>> = store.hide().map { it.contacts }
    val createEnabled: Observable<Boolean> = store.hide().map { it.createGroupEnabled }
    val progressDialog: Observable<Boolean> = store.hide().map { it.loading }.distinctUntilChanged()
    val navToChat: Observable<SingleEvent<Chat>> = navToChatCommand.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is GroupCompleteUDF.SideEffect.CreateGroup -> {
                    createGroup(sideEffect.contacts)
                }
                is GroupCompleteUDF.SideEffect.NavigateToChat -> {
                    chatCreatedPublisher.accept(sideEffect.chat)
                    navToChatCommand.accept(SingleEvent(sideEffect.chat))
                }
                is GroupCompleteUDF.SideEffect.ShowCreateGroupError -> {
                    val message = sideEffect.error.humanMessage(resourceManager)
                    showDialogCommand.accept(SingleEvent(message))
                }
            }
        }
        compositeDisposable.add(store)
    }

    private fun createGroup(contacts: List<Contact>) {
        httpApi
            .postCreateChat(storeInfo.toCreateChatRequest(contacts))
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(GroupCompleteUDF.Action.CreateGroupSuccess(it)) },
                { store.accept(GroupCompleteUDF.Action.CreateGroupError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    fun onPhotoClick() {

    }

    fun onGroupNameChanged(name: String) {
        store.accept(GroupCompleteUDF.Action.GroupNameChanged(name))
    }

    fun onContactDelete(contact: Contact) {
        store.accept(GroupCompleteUDF.Action.DeleteContact(contact))
        contactDeletePublisher.accept(contact)
    }

    fun onCreateGroup() {
        store.accept(GroupCompleteUDF.Action.CreateGroup)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
