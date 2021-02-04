package gb.smartchat.ui.chat

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import gb.smartchat.data.Repository
import gb.smartchat.di.InstanceFactory
import gb.smartchat.entity.Message
import gb.smartchat.ui.chat.state_machine.Action
import gb.smartchat.ui.chat.state_machine.SideEffect
import gb.smartchat.ui.chat.state_machine.Store
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class ChatViewModel(
    private val store: Store = Store("77f21ecc-0d4a-4f85-9173-55acf327f007"),
    private val repository: Repository = InstanceFactory.createRepository()
) : ViewModel() {

    companion object {
        const val TAG = "ChatViewModel"
    }

    private val compositeDisposable = CompositeDisposable()

    val chatList = MutableLiveData<List<ChatItem>>(emptyList())

    init {

        store.sideEffectListener = { sideEffect ->
            when(sideEffect) {
                is SideEffect.SendMessage -> sendMessage(sideEffect.message)
            }
        }

        compositeDisposable.add(
            Observable.wrap(store)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    Log.d(TAG, "viewState: $state")
                    chatList.value = state.chatItems
                }
        )
        compositeDisposable.add(
            repository.observeNewMessages()
                .subscribe { message ->
                    Log.d(TAG, "new message: $message")
                    store.accept(Action.ServerNewMessage(message))
                }
        )
    }

    fun onStart() {
        repository.connect()
    }

    fun onSendClick(text: String) {
        store.accept(Action.ClientSendMessage(text))
    }

    private fun sendMessage(message: Message) {
        val d = repository.sendMessage(message)
            .subscribe(
                { result ->
                    if (result) store.accept(Action.ServerMessageSent(message))
                    else store.accept(Action.ServerMessageSendError(message))
                },
                { e ->
                    Log.e(TAG, "sendMessage: error", e)
                    store.accept(Action.ServerMessageSendError(message))
                }
            )
        compositeDisposable.add(d)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        repository.disconnect()
    }

}