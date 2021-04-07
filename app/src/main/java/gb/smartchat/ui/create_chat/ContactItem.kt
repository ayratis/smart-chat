package gb.smartchat.ui.create_chat

sealed class ContactItem {
    data class Contact(val contact: gb.smartchat.entity.Contact) : ContactItem()
    data class Group(val group: gb.smartchat.entity.Group) : ContactItem()
}