package gb.smartchat.library.publisher

import androidx.annotation.MainThread
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.library.entity.Contact
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer

class AddRecipientsPublisher: Observable<List<Contact>>(), Consumer<List<Contact>> {

    private val publisher = PublishRelay.create<List<Contact>>()

    override fun subscribeActual(observer: Observer<in List<Contact>>) {
        publisher.hide().observeOn(AndroidSchedulers.mainThread()).subscribe(observer)
    }

    @MainThread
    override fun accept(t: List<Contact>) {
        publisher.accept(t)
    }
}
