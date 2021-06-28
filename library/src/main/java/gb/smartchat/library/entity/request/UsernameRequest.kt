package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName

data class UsernameRequest(
    @SerializedName("name")
    val name: String
)
