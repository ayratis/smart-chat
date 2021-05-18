package gb.smartchat.ui.user_profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.BehaviorRelay
import gb.smartchat.R
import gb.smartchat.data.content.ContentHelper
import gb.smartchat.data.http.HttpApi
import gb.smartchat.data.resources.ResourceManager
import gb.smartchat.publisher.UserAvatarChangedPublisher
import gb.smartchat.utils.SingleEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody

class UserProfileViewModel(
    private val httpApi: HttpApi,
    private val contentHelper: ContentHelper,
    private val userAvatarChangedPublisher: UserAvatarChangedPublisher,
    private val resourceManager: ResourceManager
) : ViewModel() {

    private var uploadDisposable: Disposable? = null

    private val avatarStateBehavior = BehaviorRelay.createDefault<AvatarState>(AvatarState.Empty)
    private val showDialogCommand = BehaviorRelay.create<SingleEvent<String>>()

    val avatarState: Observable<AvatarState> = avatarStateBehavior.hide()
    val showDialog: Observable<SingleEvent<String>> = showDialogCommand.hide()

    fun uploadAvatar(contentUri: Uri) {
        uploadDisposable?.dispose()
        val (name, size) = contentHelper.nameSize(contentUri) ?: return
        val filePart = MultipartBody.Part
            .createFormData("upload_file", name, contentHelper.requestBody(contentUri))
        httpApi
            .postUploadUserAvatar(filePart)
            .map { it.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { avatarStateBehavior.accept(AvatarState.Uploading(contentUri)) }
            .subscribe(
                {
                    avatarStateBehavior.accept(AvatarState.UploadSuccess(contentUri, it))
                    it.url?.let(userAvatarChangedPublisher::accept)
                },
                {
                    avatarStateBehavior.accept(AvatarState.Empty)
                    val message = resourceManager.getString(R.string.upload_photo_error)
                    showDialogCommand.accept(SingleEvent(message))
                }
            ).also {
                uploadDisposable = it
            }
    }

    override fun onCleared() {
        uploadDisposable?.dispose()
    }
}
