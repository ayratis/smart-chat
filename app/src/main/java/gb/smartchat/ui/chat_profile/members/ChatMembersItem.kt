package gb.smartchat.ui.chat_profile.members

import gb.smartchat.entity.Contact

sealed class ChatMembersItem {
    data class Data(val contact: Contact) : ChatMembersItem()
    object Progress : ChatMembersItem()
    data class Error(
        val message: String?,
        val action: String?,
        val tag: String?
    ) : ChatMembersItem()
}
