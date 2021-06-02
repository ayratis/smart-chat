package gb.smartchat.ui.group_complete

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.content.ContentHelper
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Contact
import gb.smartchat.entity.StoreInfo
import gb.smartchat.entity.UserProfile
import gb.smartchat.entity.request.CreateChatRequest
import gb.smartchat.publisher.ChatCreatedPublisher
import gb.smartchat.publisher.ContactDeletePublisher
import gb.smartchat.utils.SingleEvent
import gb.smartchat.utils.humanMessage
import gb.smartchat.utils.toContact
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody

class GroupCompleteViewModel(
    private val storeInfo: StoreInfo?,
    private val userProfile: UserProfile,
    private val store: GroupCompleteUDF.Store,
    private val httpApi: HttpApi,
    private val resourceManager: ResourceManager,
    private val chatCreatedPublisher: ChatCreatedPublisher,
    private val contactDeletePublisher: ContactDeletePublisher,
    private val contentHelper: ContentHelper
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private var uploadDisposable: Disposable? = null
    private val navToChatCommand = BehaviorRelay.create<SingleEvent<Chat>>()
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()

    val contacts: Observable<List<Contact>> = store.hide().map { it.contacts }
    val createEnabled: Observable<Boolean> = store.hide().map { state ->
        state.contacts.isNotEmpty() &&
                state.groupName.isNotBlank() &&
                state.avatarState !is GroupCompleteUDF.AvatarState.Uploading
    }
    val avatar: Observable<GroupCompleteUDF.AvatarState> = store.hide().map { it.avatarState }
    val progressDialog: Observable<Boolean> = store.hide().map { it.loading }.distinctUntilChanged()
    val navToChat: Observable<SingleEvent<Chat>> = navToChatCommand.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()

    init {
        store.sideEffectListener = { sideEffect ->
            when (sideEffect) {
                is GroupCompleteUDF.SideEffect.CreateGroup -> {
                    createGroup(sideEffect.groupName, sideEffect.photoUrl, sideEffect.contacts)
                }
                is GroupCompleteUDF.SideEffect.NavigateToChat -> {
                    chatCreatedPublisher.accept(sideEffect.chat)
                    navToChatCommand.accept(SingleEvent(sideEffect.chat))
                }
                is GroupCompleteUDF.SideEffect.ShowCreateGroupError -> {
                    val message = sideEffect.error.humanMessage(resourceManager)
                    showDialogCommand.accept(SingleEvent(message))
                }
                is GroupCompleteUDF.SideEffect.UploadAvatar -> {
                    uploadAvatar(sideEffect.contentUri)
                }
                is GroupCompleteUDF.SideEffect.ShowUploadAvatarError -> {
                    val message = resourceManager.getString(R.string.upload_photo_error)
                    showDialogCommand.accept(SingleEvent(message))
                }
            }
        }
        compositeDisposable.add(store)
    }

    private fun createGroup(chatName: String, avatarUrl: String?, contacts: List<Contact>) {
        val requestBody = CreateChatRequest(
            chatName = chatName,
            fileUrl = avatarUrl,
            contacts = contacts + userProfile.toContact(),
            storeId = storeInfo?.storeId,
            storeName = storeInfo?.storeName,
            partnerName = storeInfo?.partnerName,
            agentCode = storeInfo?.agentCode,
        )
        httpApi
            .postCreateChat(requestBody)
            .map { it.result.chat }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(GroupCompleteUDF.Action.CreateGroupSuccess(it)) },
                { store.accept(GroupCompleteUDF.Action.CreateGroupError(it)) }
            )
            .also { compositeDisposable.add(it) }
    }

    private fun uploadAvatar(contentUri: Uri) {
        uploadDisposable?.dispose()
        val (name, size) = contentHelper.nameSize(contentUri) ?: return
        val filePart = MultipartBody.Part
            .createFormData("upload_file", name, contentHelper.requestBody(contentUri))
        httpApi
            .postUploadChatAvatar(filePart)
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { store.accept(GroupCompleteUDF.Action.UploadAvatarSuccess(contentUri, it)) },
                { store.accept(GroupCompleteUDF.Action.UploadAvatarError(contentUri, it)) }
            ).also {
                uploadDisposable = it
                compositeDisposable.add(it)
            }
    }

    fun attachAvatar(uri: Uri) {
        store.accept(GroupCompleteUDF.Action.AttachAvatar(uri))
    }

    fun onGroupNameChanged(name: String) {
        store.accept(GroupCompleteUDF.Action.GroupNameChanged(name))
    }

    fun onContactDelete(contact: Contact) {
        store.accept(GroupCompleteUDF.Action.DeleteContact(contact))
        contactDeletePublisher.accept(contact)
    }

    fun onCreateGroup() {
        store.accept(GroupCompleteUDF.Action.CreateGroup)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}
