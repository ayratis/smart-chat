package gb.smartchat.library.publisher

import androidx.annotation.MainThread
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.library.entity.Chat
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer

class ChatEditedPublisher : Observable<Chat>(), Consumer<Chat> {

    private val publisher = PublishRelay.create<Chat>()

    override fun subscribeActual(observer: Observer<in Chat>) {
        publisher.hide().observeOn(AndroidSchedulers.mainThread()).subscribe(observer)
    }

    @MainThread
    override fun accept(t: Chat) {
        publisher.accept(t)
    }
}
