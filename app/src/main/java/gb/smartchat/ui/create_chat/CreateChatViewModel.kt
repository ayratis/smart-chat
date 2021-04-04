package gb.smartchat.ui.create_chat

import androidx.lifecycle.ViewModel
import gb.smartchat.data.http.HttpApi
import io.reactivex.disposables.CompositeDisposable

class CreateChatViewModel(
    private val httpApi: HttpApi
) : ViewModel() {

    companion object {
        private const val TAG = "CreateChatViewModel"
    }

    private val compositeDisposable = CompositeDisposable()

    private fun fetchContacts() {

    }
}