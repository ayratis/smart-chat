package gb.smartchat.library.data.connection

import io.reactivex.Observable

interface ConnectionManager {
    val isOnline: Observable<Boolean>
}
