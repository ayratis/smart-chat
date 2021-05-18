package gb.smartchat.ui.user_profile

import android.net.Uri
import gb.smartchat.entity.File

sealed class AvatarState {
    object Empty : AvatarState()
    data class Uploading(val uri: Uri) : AvatarState()
    data class UploadSuccess(val uri: Uri, val file: File) : AvatarState()
}
