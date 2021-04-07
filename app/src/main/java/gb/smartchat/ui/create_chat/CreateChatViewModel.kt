package gb.smartchat.ui.create_chat

import androidx.lifecycle.ViewModel
import gb.smartchat.data.http.HttpApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CreateChatViewModel(
    private val httpApi: HttpApi,
    private val store: CreateChatUDF.Store
) : ViewModel() {

    companion object {
        private const val TAG = "CreateChatViewModel"
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
        return when (this) {
            is CreateChatUDF.State.Data -> {
                val list = mutableListOf<ContactItem>()
                for (group in this.groups) {
                    list += ContactItem.Group(group)
                    list += group.contacts.map { ContactItem.Contact(it) }
                }
                list
            }
            else -> emptyList()
        }
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
