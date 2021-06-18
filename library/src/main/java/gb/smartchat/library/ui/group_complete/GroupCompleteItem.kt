package gb.smartchat.library.ui.group_complete

sealed class GroupCompleteItem {
    data class Info(
        val photoUrl: String?,
        val groupName: String,
        val memberCount: Int
    ) : GroupCompleteItem()

    data class Contact(val contact: gb.smartchat.library.entity.Contact) : GroupCompleteItem()
}
