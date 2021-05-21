package gb.smartchat.entity


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserProfile(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("avatar")
    val avatar: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("favorite_chat_id")
    val favoriteChatId: Long?
) : Serializable
