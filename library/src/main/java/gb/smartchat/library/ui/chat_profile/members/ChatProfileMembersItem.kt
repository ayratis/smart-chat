package gb.smartchat.library.ui.chat_profile.members

import gb.smartchat.library.entity.Contact

sealed class ChatProfileMembersItem {
    data class Data(val contact: Contact) : ChatProfileMembersItem()
    object Progress : ChatProfileMembersItem()
    data class Error(
        val message: String?,
        val action: String?,
        val tag: String?
    ) : ChatProfileMembersItem()
}
