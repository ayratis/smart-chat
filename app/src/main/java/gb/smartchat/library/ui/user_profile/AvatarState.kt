package gb.smartchat.library.ui.user_profile

import android.net.Uri
import gb.smartchat.library.entity.File

sealed class AvatarState {
    object Empty : AvatarState()
    data class Uploading(val uri: Uri) : AvatarState()
    data class UploadSuccess(val uri: Uri, val file: File) : AvatarState()
}
