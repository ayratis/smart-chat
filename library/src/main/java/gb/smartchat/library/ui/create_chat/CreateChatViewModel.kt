package gb.smartchat.library.ui.create_chat

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.entity.UserProfile
import gb.smartchat.library.entity.request.AddRecipientsRequest
import gb.smartchat.library.entity.request.CreateChatRequest
import gb.smartchat.library.publisher.AddRecipientsPublisher
import gb.smartchat.library.publisher.ChatCreatedPublisher
import gb.smartchat.library.publisher.ContactDeletePublisher
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.humanMessage
import gb.smartchat.library.utils.toContact
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CreateChatViewModel(
    private val storeInfo: StoreInfo?,
    private val userProfile: UserProfile?,
    private val chatId: Long?,
    private val mode: CreateChatMode,
    private val httpApi: HttpApi,
    private val store: CreateChatUDF.Store,
    private val resourceManager: ResourceManager,
    private val chatCreatedPublisher: ChatCreatedPublisher,
    contactDeletePublisher: ContactDeletePublisher,
    private val addRecipientsPublisher: AddRecipientsPublisher
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
                is CreateChatUDF.SideEffect.CreateChat -> {
                    createChat(sideEffect.contact)
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
            .getContactList(
                storeInfo?.storeId,
                storeInfo?.storeName,
                storeInfo?.partnerCode,
                storeInfo?.partnerName,
                storeInfo?.agentCode
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
        val myContact = userProfile!!.toContact()
        val requestBody = CreateChatRequest(
            chatName = "",
            fileUrl = null,
            contacts = listOf(contact, myContact),
            storeId = storeInfo?.storeId,
            storeName = storeInfo?.storeName,
            partnerName = storeInfo?.partnerName,
            agentCode = storeInfo?.agentCode,
        )
        httpApi
            .postCreateChat(requestBody)
            .map { it.result.chat }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(CreateChatUDF.Action.CreateChatSuccess(it)) },
                { store.accept(CreateChatUDF.Action.CreateChatError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun addMembersToChat(contacts: List<Contact>) {
        httpApi
            .postAddRecipients(
                AddRecipientsRequest(
                    chatId = chatId!!,
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
        if (mode == CreateChatMode.CREATE_SINGLE) {
            list += ContactItem.CreateGroupButton
        }
        when (this.contactsResponseState) {
            is CreateChatUDF.ContactsResponseState.Data -> {
                for (group in this.groupsToShow) {
                    if (mode == CreateChatMode.CREATE_SINGLE) {
                        list += ContactItem.Group(group)
                        list += group.contacts.map { ContactItem.Contact(it) }
                    } else {
                        list += group.contacts.map {
                            ContactItem.SelectableContact(it, selectedContacts.contains(it))
                        }
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