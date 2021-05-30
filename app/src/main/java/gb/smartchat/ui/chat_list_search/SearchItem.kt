package gb.smartchat.ui.chat_list_search

sealed class SearchItem {
    data class Header(val name: String) : SearchItem()
    data class Chat(val chat: gb.smartchat.entity.Chat) : SearchItem()
    data class Contact(val contact: gb.smartchat.entity.Contact) : SearchItem()
}
