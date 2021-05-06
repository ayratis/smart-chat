package gb.smartchat.ui.group_profile.members

import gb.smartchat.entity.Contact

sealed class GroupMembersItem {
    data class Data(val contact: Contact) : GroupMembersItem()
    object Progress : GroupMembersItem()
    data class Error(
        val message: String?,
        val action: String?,
        val tag: String?
    ) : GroupMembersItem()
}
