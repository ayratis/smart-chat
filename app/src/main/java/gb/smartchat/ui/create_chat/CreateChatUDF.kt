package gb.smartchat.ui.create_chat

object CreateChatUDF {

    sealed class State {
        object Empty : State()
        object Loading : State()
        data class Data(val items: List<String>) : State()
    }


}