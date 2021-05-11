package gb.smartchat.ui.chat_profile.files

import gb.smartchat.entity.File

sealed class ChatProfileFileItem {
    data class Media(val file: File) : ChatProfileFileItem()
    data class Doc(val file: File) : ChatProfileFileItem()
    object Progress : ChatProfileFileItem()
    data class Error(
        val message: String?,
        val action: String?,
        val tag: String?
    ) : ChatProfileFileItem()
}
