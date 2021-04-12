package gb.smartchat.publisher

import androidx.annotation.MainThread
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.entity.Contact
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer

class ContactDeletePublisher : Observable<Contact>(), Consumer<Contact> {

    private val publisher = PublishRelay.create<Contact>()

    override fun subscribeActual(observer: Observer<in Contact>) {
        publisher.hide().observeOn(AndroidSchedulers.mainThread()).subscribe(observer)
    }

    @MainThread
    override fun accept(t: Contact) {
        publisher.accept(t)
    }
}
