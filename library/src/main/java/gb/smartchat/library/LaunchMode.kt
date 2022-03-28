package gb.smartchat.library

import java.io.Serializable

sealed class LaunchMode : Serializable {
    object ChatList : LaunchMode()
    object ServiceChat : LaunchMode()
}