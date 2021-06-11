package gb.smartchat.library.ui.chat_profile.members

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.entity.request.AddRecipientsRequest
import gb.smartchat.library.publisher.AddRecipientsPublisher
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.humanMessage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ChatProfileMembersViewModel(
    private val chatId: Long,
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager,
    addRecipientsPublisher: AddRecipientsPublisher
) : ViewModel() {

    companion object {
        private const val RETRY_TAG = "retry tag"
    }

    private val compositeDisposable = CompositeDisposable()
    private var fetchDisposable: Disposable? = null
    private val state = BehaviorRelay.create<List<ChatProfileMembersItem>>()
    private val showErrorDialogCommand = BehaviorRelay.create<SingleEvent<String>>()

    val viewState: Observable<List<ChatProfileMembersItem>> = state.hide()
    val showErrorDialog: Observable<SingleEvent<String>> = showErrorDialogCommand.hide()

    init {
        fetchContacts()
        addRecipientsPublisher
            .subscribe { newRecipients ->
                state.value?.let { items ->
                    if (items.firstOrNull() is ChatProfileMembersItem.Data) {
                        state.accept(
                            items + newRecipients.map { ChatProfileMembersItem.Data(it) }
                        )
                    }
                }
            }
            .also { compositeDisposable.add(it) }
    }

    private fun fetchContacts() {
        fetchDisposable?.dispose()
        fetchDisposable = httpApi
            .getRecipients(chatId)
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                state.accept(listOf(ChatProfileMembersItem.Progress))
            }
            .subscribe(
                { contacts ->
                    val data = contacts.map { ChatProfileMembersItem.Data(it) }
                    state.accept(data)
                },
                { error ->
                    val errorItem = ChatProfileMembersItem.Error(
                        error.humanMessage(resourceManager),
                        resourceManager.getString(R.string.retry),
                        RETRY_TAG
                    )
                    state.accept(listOf(errorItem))
                }
            )
            .also {
                compositeDisposable.add(it)
            }
    }

    private fun deleteUser(contact: Contact) {
        httpApi
            .postDeleteRecipients(AddRecipientsRequest(chatId, listOf(contact)))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    state.value?.let { items ->
                        val newItems = items.filter { item ->
                            if (item is ChatProfileMembersItem.Data) {
                                return@filter item.contact.id != contact.id
                            }
                            true
                        }
                        state.accept(newItems)
                    }
                },
                {
                    val message = resourceManager.getString(R.string.delete_member_error)
                    showErrorDialogCommand.accept(SingleEvent(message))
                }
            )
            .also { compositeDisposable.add(it) }
    }

    fun onErrorActionClick(tag: String) {
        if (tag == RETRY_TAG) {
            fetchContacts()
        }
    }

    fun onDeleteUserClick(contact: Contact) {
        deleteUser(contact)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
