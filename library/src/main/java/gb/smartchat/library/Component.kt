package gb.smartchat.library

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import gb.smartchat.library.data.content.ContentHelper
import gb.smartchat.library.data.content.ContentHelperImpl
import gb.smartchat.library.data.download.FileDownloadHelper
import gb.smartchat.library.data.download.FileDownloadHelperImpl
import gb.smartchat.library.data.gson.GsonDateAdapter
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.data.resources.ResourceManagerImpl
import gb.smartchat.library.data.socket.SocketApi
import gb.smartchat.library.data.socket.SocketApiImpl
import gb.smartchat.library.data.socket.SocketEvent
import gb.smartchat.library.entity.Message
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.entity.UserProfile
import gb.smartchat.library.publisher.*
import gb.smartchat.library.utils.SingleEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.time.ZonedDateTime

//uses as di component (singletone for library created by activity)
class Component constructor(
    private val application: Application,
    private val baseUrl: String,
    val userId: String,
    val storeInfoList: List<StoreInfo>,
) : ViewModel() {

    class Factory(
        private val application: Application,
        private val baseUrl: String,
        private val userId: String,
        private val storeInfoList: List<StoreInfo>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return Component(application, baseUrl, userId, storeInfoList) as T
        }
    }

    var userProfile: UserProfile? = null

    val resourceManager: ResourceManager by lazy {
        ResourceManagerImpl(application.resources)
    }

    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, GsonDateAdapter())
            .create()
    }

    val okHttpClient: OkHttpClient by lazy {
        with(OkHttpClient.Builder()) {
            addInterceptor {
                val request = it.request()
                val newRequest =
                    request.newBuilder().addHeader("smart-user-id", userId)
                        .build()
                it.proceed(newRequest)
            }
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            build()
        }
    }

    val socket: Socket by lazy {
        val options = IO.Options().apply {
            callFactory = okHttpClient
            webSocketFactory = okHttpClient
        }
        val manager = Manager(URI(baseUrl), options)
        val socket = manager.socket("/chat_v1").apply {
            io().timeout(-1)
        }
        socket
    }

    val socketApi: SocketApi by lazy {
        SocketApiImpl(socket, gson)
    }

    val httpApi: HttpApi by lazy {
        with(Retrofit.Builder()) {
            addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            addConverterFactory(GsonConverterFactory.create(gson))
            client(okHttpClient)
            baseUrl(baseUrl)
            build().create(HttpApi::class.java)
        }
    }

    val contentHelper: ContentHelper by lazy {
        ContentHelperImpl(application)
    }

    val fileDownloadHelper: FileDownloadHelper by lazy {
        FileDownloadHelperImpl(application)
    }

    val contactDeletePublisher: ContactDeletePublisher by lazy {
        ContactDeletePublisher()
    }

    val chatCreatedPublisher: ChatCreatedPublisher by lazy {
        ChatCreatedPublisher()
    }

    val messageReadInternalPublisher: MessageReadInternalPublisher by lazy {
        MessageReadInternalPublisher()
    }

    val chatUnreadMessageCountPublisher by lazy {
        ChatUnreadMessageCountPublisher()
    }

    val chatUnarchivePublisher by lazy {
        ChatUnarchivePublisher()
    }

    val addRecipientsPublisher by lazy {
        AddRecipientsPublisher()
    }

    val userAvatarChangedPublisher by lazy {
        UserAvatarChangedPublisher()
    }

    val leaveChatPublisher by lazy {
        LeaveChatPublisher()
    }

    val chatArchivePublisher by lazy {
        ChatArchivePublisher()
    }

    private var newMessagesDisposable: Disposable? = null
    private val sendNewMessagePushCommand = PublishRelay.create<Message>()
    private val userMissingCommand = BehaviorRelay.create<SingleEvent<Unit>>()
    private val usernameMissingCommand = BehaviorRelay.create<SingleEvent<Unit>>()

    val sendNewMessagePush: Observable<Message> = sendNewMessagePushCommand.hide()
    val userMissing: Observable<SingleEvent<Unit>> = userMissingCommand.hide()
    val usernameMissing: Observable<SingleEvent<Unit>> = usernameMissingCommand.hide()

    private fun observeNewMessages() {
        newMessagesDisposable?.dispose()
        newMessagesDisposable = socketApi
            .observeEvents()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { socketEvent ->
                when(socketEvent) {
                    is SocketEvent.MessageNew -> {
                        sendNewMessagePushCommand.accept(socketEvent.message)
                    }
                    is SocketEvent.UserMissing -> {
                        userMissingCommand.accept(SingleEvent(Unit))
                    }
                    is SocketEvent.UsernameMissing -> {
                        usernameMissingCommand.accept(SingleEvent(Unit))
                    }
                }
            }
    }

    override fun onCleared() {
        newMessagesDisposable?.dispose()
        socket.disconnect()
    }

    fun connect() {
        socketApi.connect()
        observeNewMessages()
    }
}
