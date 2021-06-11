package gb.smartchat.library.ui.chat_list_search

sealed class SearchItem {
    data class Header(val name: String) : SearchItem()
    data class Chat(val chat: gb.smartchat.library.entity.Chat) : SearchItem()
    data class Contact(val contact: gb.smartchat.library.entity.Contact) : SearchItem()
}
