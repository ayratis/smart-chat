package gb.smartchat.library.ui.chat_profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.library.data.content.ContentHelper
import gb.smartchat.library.data.http.HttpApi
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.request.AddRecipientsRequest
import gb.smartchat.library.entity.request.PinChatRequest
import gb.smartchat.library.publisher.ChatArchivePublisher
import gb.smartchat.library.publisher.ChatEditedPublisher
import gb.smartchat.library.publisher.ChatUnarchivePublisher
import gb.smartchat.library.publisher.LeaveChatPublisher
import gb.smartchat.library.utils.SingleEvent
import gb.smartchat.library.utils.toContact
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody

class ChatProfileViewModel(
    private val httpApi: HttpApi,
    chatArg: Chat,
    private val userId: String,
    private val resourceManager: ResourceManager,
    private val leaveChatPublisher: LeaveChatPublisher,
    private val chatArchivePublisher: ChatArchivePublisher,
    private val chatUnarchivePublisher: ChatUnarchivePublisher,
    private val contentHelper: ContentHelper,
    private val chatEditedPublisher: ChatEditedPublisher
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private var uploadDisposable: Disposable? = null

    private var chat = BehaviorRelay.createDefault(chatArg)
    private val avatarBehavior = BehaviorRelay.create<Uri>()
    private val photoUploadingBehavior = BehaviorRelay.createDefault(false)
    private val exitToRootScreenCommand = BehaviorRelay.create<SingleEvent<Unit>>()
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()
    private val navigateToAddContactsCommand = BehaviorRelay.create<SingleEvent<Chat>>()

    val avatar: Observable<Uri> = avatarBehavior.hide()
    val photoUploading: Observable<Boolean> = photoUploadingBehavior.hide()
    val memberCount: Observable<Int> = chat.hide().map { it.users.size }
    val exitToRootScreen: Observable<SingleEvent<Unit>> = exitToRootScreenCommand.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()
    val navigateToAddContacts: Observable<SingleEvent<Chat>> = navigateToAddContactsCommand.hide()

    init {
        chatArg.avatar?.let { url ->
            avatarBehavior.accept(Uri.parse(url))
        }
        chatEditedPublisher
            .subscribe { editedChat ->
                chat.accept(editedChat)
            }
            .also { compositeDisposable.add(it) }
    }

    fun leaveChat() {
        val me = chat.value!!.users.find { it.id == userId } ?: return
        httpApi
            .postLeaveChat(AddRecipientsRequest(chat.value!!.id, listOf(me.toContact())))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    leaveChatPublisher.accept(chat.value!!)
                    exitToRootScreenCommand.accept(SingleEvent(Unit))
                },
                {
                    val message = resourceManager.getString(R.string.leave_chat_error)
                    showDialogCommand.accept(SingleEvent(message))
                }
            )
            .also { compositeDisposable.add(it) }
    }

    fun archiveChat() {
        httpApi
            .postArchiveChat(PinChatRequest(chatId = chat.value!!.id))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chatArchivePublisher.accept(chat.value!!)
                    exitToRootScreenCommand.accept(SingleEvent(Unit))
                },
                {
                    val message = resourceManager.getString(R.string.archive_chat_error)
                    showDialogCommand.accept(SingleEvent(message))
                }
            )
            .also { compositeDisposable.add(it) }
    }

    fun unarchiveChat() {
        httpApi
            .postUnarchiveChat(PinChatRequest(chatId = chat.value!!.id))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chatUnarchivePublisher.accept(chat.value!!)
                    exitToRootScreenCommand.accept(SingleEvent(Unit))
                },
                {
                    val message = resourceManager.getString(R.string.archive_chat_error)
                    showDialogCommand.accept(SingleEvent(message))
                }
            )
            .also { compositeDisposable.add(it) }
    }

    fun editPhoto(uri: Uri) {
        uploadDisposable?.dispose()
        val (name, size) = contentHelper.nameSize(uri) ?: return
        val filePart = MultipartBody.Part
            .createFormData("upload_file", name, contentHelper.requestBody(uri))
        httpApi
            .postUploadChatAvatar(filePart)
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                avatarBehavior.accept(uri)
                photoUploadingBehavior.accept(true)
            }
            .doFinally { photoUploadingBehavior.accept(false) }
            .subscribe(
                { file ->
                    chatEditedPublisher.accept(
                        chat.value!!.copy(avatar = file.url)
                    )
                },
                {
                    showDialogCommand.accept(
                        SingleEvent(resourceManager.getString(R.string.upload_photo_error))
                    )
                }
            ).also {
                uploadDisposable = it
                compositeDisposable.add(it)
            }
    }

    fun addContacts() {
        navigateToAddContactsCommand.accept(SingleEvent(chat.value!!))
    }

    override fun onCleared() {
        compositeDisposable.clear()
    }
}
