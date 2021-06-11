package gb.smartchat.library.ui.create_chat

sealed class ContactItem {
    object CreateGroupButton : ContactItem()
    object Loading : ContactItem()
    data class Error(val message: String?, val action: String?, val tag: String?) : ContactItem()
    data class Group(val group: gb.smartchat.library.entity.Group) : ContactItem()
    data class Contact(val contact: gb.smartchat.library.entity.Contact) : ContactItem()
    data class SelectableContact(
        val contact: gb.smartchat.library.entity.Contact,
        val isSelected: Boolean
    ) : ContactItem()
}
