package gb.smartchat.ui.create_chat

sealed class ContactItem {
    object CreateGroupButton : ContactItem()
    object Loading : ContactItem()
    data class Error(val message: String?, val action: String?, val tag: String?) : ContactItem()
    data class Group(val group: gb.smartchat.entity.Group) : ContactItem()
    data class Contact(val contact: gb.smartchat.entity.Contact) : ContactItem()
    data class SelectableContact(
        val contact: gb.smartchat.entity.Contact,
        val isSelected: Boolean
    ) : ContactItem()
}
