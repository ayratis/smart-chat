package gb.smartchat.library.data.resources

import androidx.annotation.StringRes

interface ResourceManager {
    fun getString(@StringRes stringRes: Int): String
}
