package gb.smartchat.publisher

import androidx.annotation.MainThread
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer

class UserAvatarChangedPublisher : Observable<String>(), Consumer<String> {

    private val publisher = PublishRelay.create<String>()

    override fun subscribeActual(observer: Observer<in String>) {
        publisher.hide().observeOn(AndroidSchedulers.mainThread()).subscribe(observer)
    }

    @MainThread
    override fun accept(t: String) {
        publisher.accept(t)
    }
}
