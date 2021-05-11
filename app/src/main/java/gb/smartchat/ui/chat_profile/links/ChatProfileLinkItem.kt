package gb.smartchat.ui.chat_profile.links

sealed class ChatProfileLinkItem {
    data class Data(val link: String) : ChatProfileLinkItem()
    object Progress : ChatProfileLinkItem()
    data class Error(
        val message: String?,
        val action: String?,
        val tag: String?
    ) : ChatProfileLinkItem()
}
