package gb.smartchat.publisher

import androidx.annotation.MainThread
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer

//chat id and unreadCount
class ChatUnreadMessageCountPublisher: Observable<Pair<Long, Int>>(), Consumer<Pair<Long, Int>> {

    private val publisher = PublishRelay.create<Pair<Long, Int>>()

    override fun subscribeActual(observer: Observer<in Pair<Long, Int>>) {
        publisher.hide().observeOn(AndroidSchedulers.mainThread()).subscribe(observer)
    }

    @MainThread
    override fun accept(t: Pair<Long, Int>) {
        publisher.accept(t)
    }
}
