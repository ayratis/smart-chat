package gb.smartchat.library.ui.create_chat

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.entity.Group
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.entity.request.AddRecipientsRequest
import gb.smartchat.library.publisher.AddRecipientsPublisher
import gb.smartchat.library.publisher.ChatCreatedPublisher
import gb.smartchat.library.publisher.ContactDeletePublisher
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.humanMessage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CreateChatViewModel(
    private val storeInfo: StoreInfo?,
    private val chat: Chat?,
    private val httpApi: HttpApi,
    private val store: CreateChatUDF.Store,
    private val resourceManager: ResourceManager,
    private val chatCreatedPublisher: ChatCreatedPublisher,
    contactDeletePublisher: ContactDeletePublisher,
    private val addRecipientsPublisher: AddRecipientsPublisher,
    private val createChatMode: CreateChatMode
) : ViewModel() {

    companion object {
        private const val TAG = "CreateChatViewModel"
        private const val ERROR_RETRY_TAG = "error retry tag"
    }

    private val compositeDisposable = CompositeDisposable()
    private val state: Observable<CreateChatUDF.State> = Observable.wrap(store)
    private val navToChatCommand = BehaviorRelay.create<SingleEvent<Chat>>()
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()
    private val navToGroupCompleteCommand =
        BehaviorRelay.create<SingleEvent<Pair<StoreInfo?, List<Contact>>>>()
    private val exitCommand = BehaviorRelay.create<SingleEvent<Unit>>()

    val items: Observable<List<ContactItem>> = state
        .map { it.mapIntoContactItems() }
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
    val progressDialog: Observable<Boolean> = state.hide().map { it.blockingProgress }
    val selectedCount: Observable<Pair<Int, Int>> = state.hide().map {
        val selectedCount = it.selectedContacts.size
        val totalCount = (it.contactsResponseState as? CreateChatUDF.ContactsResponseState.Data)
            ?.let { data ->
                var size = 0
                for (group in data.groups) {
                    size += group.contacts.size
                }
                size
            }
            ?: 0
        selectedCount to totalCount
    }
    val navToChat: Observable<SingleEvent<Chat>> = navToChatCommand.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()
    val navToGroupComplete: Observable<SingleEvent<Pair<StoreInfo?, List<Contact>>>> =
        navToGroupCompleteCommand.hide()
    val exit: Observable<SingleEvent<Unit>> = exitCommand.hide()

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                CreateChatUDF.SideEffect.LoadContacts -> {
                    fetchContacts()
                }
                is CreateChatUDF.SideEffect.NavigateToChat -> {
                    chatCreatedPublisher.accept(sideEffect.chat)
                    navToChatCommand.accept(SingleEvent(sideEffect.chat))
                }
                is CreateChatUDF.SideEffect.ShowErrorMessage -> {
                    val message = sideEffect.error.humanMessage(resourceManager)
                    showDialogCommand.accept(SingleEvent(message))
                }
                is CreateChatUDF.SideEffect.NavigateToGroupComplete -> {
                    navToGroupCompleteCommand.accept(
                        SingleEvent(storeInfo to sideEffect.selectedContacts)
                    )
                }
                is CreateChatUDF.SideEffect.AddMembersToChat -> {
                    addMembersToChat(sideEffect.selectedContacts)
                }
                CreateChatUDF.SideEffect.Exit -> {
                    exitCommand.accept(SingleEvent(Unit))
                }
            }
        }
        store.accept(CreateChatUDF.Action.Refresh)
        compositeDisposable.add(store)
        contactDeletePublisher
            .subscribe { store.accept(CreateChatUDF.Action.DeleteContact(it)) }
            .also { compositeDisposable.add(it) }
    }

    private fun fetchContacts() {
        httpApi
            .getContactList(storeInfo?.storeId)
            .map { it.result.groups ?: emptyList() }
            .map { groups ->
                if (createChatMode == CreateChatMode.ADD_MEMBERS) {
                    val alreadyAddedUserIds = chat!!.users.map { it.id }
                    val filteredGroups = mutableListOf<Group>()
                    for (group in groups) {
                        val filteredContacts = group.contacts.filter { contact ->
                            !alreadyAddedUserIds.contains(contact.id)
                        }
                        if (filteredContacts.isNotEmpty()) {
                            filteredGroups += group.copy(contacts = filteredContacts)
                        }
                    }
                    filteredGroups
                } else {
                    groups
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(CreateChatUDF.Action.LoadContactsSuccess(it)) },
                { store.accept(CreateChatUDF.Action.LoadContactsError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun addMembersToChat(contacts: List<Contact>) {
        httpApi
            .postAddRecipients(
                AddRecipientsRequest(
                    chatId = chat!!.id,
                    contacts = contacts
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    addRecipientsPublisher.accept(contacts)
                    store.accept(CreateChatUDF.Action.AddMembersSuccess)
                },
                { store.accept(CreateChatUDF.Action.AddMembersError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun CreateChatUDF.State.mapIntoContactItems(): List<ContactItem> {
        val list = mutableListOf<ContactItem>()
        when (this.contactsResponseState) {
            is CreateChatUDF.ContactsResponseState.Data -> {
                for (group in this.groupsToShow) {
                    list += ContactItem.Group(group)
                    list += group.contacts.map {
                        ContactItem.SelectableContact(it, selectedContacts.contains(it))
                    }
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
        store.accept(CreateChatUDF.Action.OnContactClick(contact))
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

    fun onCreateGroupNextClick() {
        store.accept(CreateChatUDF.Action.DoneClick)
    }
}
