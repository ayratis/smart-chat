package gb.smartchat.ui.create_chat

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Contact
import gb.smartchat.entity.StoreInfo
import gb.smartchat.utils.SingleEvent
import gb.smartchat.utils.humanMessage
import gb.smartchat.utils.toContact
import gb.smartchat.utils.toCreateChatRequest
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CreateChatViewModel(
    private val storeInfo: StoreInfo,
    private val httpApi: HttpApi,
    private val store: CreateChatUDF.Store,
    private val resourceManager: ResourceManager
) : ViewModel() {

    companion object {
        private const val TAG = "CreateChatViewModel"
        private const val ERROR_RETRY_TAG = "error retry tag"
    }

    private val compositeDisposable = CompositeDisposable()
    private val state: Observable<CreateChatUDF.State> = Observable.wrap(store)
    private val navToChatCommand = BehaviorRelay.create<SingleEvent<Chat>>()
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()

    val items: Observable<List<ContactItem>> = state
        .map { it.mapIntoContactItems() }
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
    val progressDialog: Observable<Boolean> = state.hide().map { it.createChatProgress }
    val navToChat: Observable<SingleEvent<Chat>> = navToChatCommand.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                CreateChatUDF.SideEffect.LoadContacts -> {
                    fetchContacts()
                }
                is CreateChatUDF.SideEffect.CreateChat -> {
                    createChat(sideEffect.contact)
                }
                is CreateChatUDF.SideEffect.NavigateToChat -> {
                    navToChatCommand.accept(SingleEvent(sideEffect.chat))
                }
                is CreateChatUDF.SideEffect.ShowErrorMessage -> {
                    val message = sideEffect.error.humanMessage(resourceManager)
                    showDialogCommand.accept(SingleEvent(message))
                }
            }
        }
        store.accept(CreateChatUDF.Action.Refresh)
    }

    private fun fetchContacts() {
        httpApi
            .getContactList(
                storeInfo.storeId,
                storeInfo.storeName,
                storeInfo.partnerCode,
                storeInfo.partnerName,
                storeInfo.agentCode
            )
            .map { it.result.groups ?: emptyList() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(CreateChatUDF.Action.LoadContactsSuccess(it)) },
                { store.accept(CreateChatUDF.Action.LoadContactsError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun createChat(contact: Contact) {
        val contacts = listOf(storeInfo.userProfile.toContact(), contact)
        httpApi
            .postCreateChat(storeInfo.toCreateChatRequest(contacts))
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(CreateChatUDF.Action.CreateChatSuccess(it)) },
                { store.accept(CreateChatUDF.Action.CreateChatError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun CreateChatUDF.State.mapIntoContactItems(): List<ContactItem> {
        val list = mutableListOf<ContactItem>()
        list += ContactItem.CreateGroupButton
        when (this.contactsResponseState) {
            is CreateChatUDF.ContactsResponseState.Data -> {
                for (group in this.groupsToShow) {
                    list += ContactItem.Group(group)
                    list += group.contacts.map { ContactItem.Contact(it) }
                }
            }
            is CreateChatUDF.ContactsResponseState.Error -> {
                list += ContactItem.Error(
                    message = this.contactsResponseState.error.humanMessage(resourceManager),
                    action = resourceManager.getString(R.string.retry),
                    tag = ERROR_RETRY_TAG
                )
            }
            is CreateChatUDF.ContactsResponseState.Loading -> {
                list += ContactItem.Loading
            }
        }
        return list
    }

    fun onErrorActionClick(tag: String) {
        when (tag) {
            ERROR_RETRY_TAG -> store.accept(CreateChatUDF.Action.Refresh)
        }
    }

    fun onContactClick(contact: Contact) {
        store.accept(CreateChatUDF.Action.CreateChatClick(contact))
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }

    fun onQueryTextSubmit(query: String?) {
        store.accept(CreateChatUDF.Action.QueryTextSubmit(query))
    }

    fun onQueryTextChange(query: String?) {
        store.accept(CreateChatUDF.Action.QueryTextChanged(query))
    }
}
