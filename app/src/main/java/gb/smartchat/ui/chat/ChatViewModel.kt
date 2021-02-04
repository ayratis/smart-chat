package gb.smartchat.ui.chat

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import gb.smartchat.data.InstanceFactory
import gb.smartchat.data.Repository
import io.reactivex.disposables.CompositeDisposable

class ChatViewModel(
    private val repository: Repository = InstanceFactory.createRepository()
) : ViewModel() {

    companion object {
        const val TAG = "ChatViewModel"
    }

    private val compositeDisposable = CompositeDisposable()
    val chatList = MutableLiveData<List<ChatItem>>(emptyList())

    init {
        val d = repository.observeNewMessages()
            .subscribe { message ->
                Log.d(TAG, "new message: $message")
                addMsgToList(message.text ?: "```")
            }
        compositeDisposable.add(d)
    }

    fun onStart() {
        repository.connect()
    }

    fun onSendClick(text: String) {
        val d = repository.sendMessage(text)
            .subscribe(
                { Log.d(TAG, "onSend success: $it") },
                { Log.e(TAG, "onSendFailure: ${it.message}", it) }
            )
        compositeDisposable.add(d)
    }

    private fun addMsgToList(msg: String) {
        Log.d(TAG, msg)
        val list = chatList.value ?: listOf()
        val lastItemId = list.lastOrNull()?.id ?: 1
        val newList = list + ChatItem(lastItemId + 1, msg)
        chatList.postValue(newList)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        repository.disconnect()
    }

}