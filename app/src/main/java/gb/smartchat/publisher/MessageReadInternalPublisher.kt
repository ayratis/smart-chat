package gb.smartchat.publisher

import androidx.annotation.MainThread
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.entity.MessageRead
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer

class MessageReadInternalPublisher: Observable<MessageRead>(), Consumer<MessageRead> {

    private val publisher = PublishRelay.create<MessageRead>()

    override fun subscribeActual(observer: Observer<in MessageRead>) {
        publisher.hide().observeOn(AndroidSchedulers.mainThread()).subscribe(observer)
    }

    @MainThread
    override fun accept(t: MessageRead) {
        publisher.accept(t)
    }
}
