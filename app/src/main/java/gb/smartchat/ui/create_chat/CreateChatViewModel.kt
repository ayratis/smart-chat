package gb.smartchat.ui.create_chat

import androidx.lifecycle.ViewModel
import gb.smartchat.R
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.utils.humanMessage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CreateChatViewModel(
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

    val items: Observable<List<ContactItem>> = state
        .map { it.mapIntoContactItems() }
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                CreateChatUDF.SideEffect.LoadContacts -> fetchContacts()
            }
        }
        store.accept(CreateChatUDF.Action.Refresh)
    }

    private fun fetchContacts() {
        httpApi
            .getContactList(
                storeId = "asd",
                storeName = "asd",
                partnerCode = 123,
                partnerName = "asd",
                agentCode = 123
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
        when(tag) {
            ERROR_RETRY_TAG -> store.accept(CreateChatUDF.Action.Refresh)
        }
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
