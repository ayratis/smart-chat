package gb.smartchat.ui.chat_profile.members

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.entity.Contact
import gb.smartchat.utils.humanMessage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ChatProfileMembersViewModel(
    private val chatId: Long,
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager
) : ViewModel() {

    companion object {
        private const val RETRY_TAG = "retry tag"
    }

    private var fetchDisposable: Disposable? = null
    private val state = BehaviorRelay.create<List<ChatProfileMembersItem>>()

    val viewState: Observable<List<ChatProfileMembersItem>> = state.hide()

    init {
        fetchContacts()
    }

    private fun fetchContacts() {
        fetchDisposable?.dispose()
        fetchDisposable = httpApi
            .getRecipients(chatId)
            .doOnSubscribe {
                state.accept(listOf(ChatProfileMembersItem.Progress))
            }
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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
    }

    fun onContactClick(contact: Contact) {
    }

    fun onErrorActionClick(tag: String) {
        if (tag == RETRY_TAG) {
            fetchContacts()
        }
    }

    override fun onCleared() {
        fetchDisposable?.dispose()
    }
}
