package gb.smartchat.ui.chat_profile.media

import gb.smartchat.entity.File

sealed class MediaItem {
    data class Data(val file: File) : MediaItem()
    object Progress : MediaItem()
    data class Error(
        val message: String?,
        val action: String?,
        val tag: String?
    ) : MediaItem()
}
