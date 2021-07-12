package gb.smartchat.library.entity


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Contact(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("avatar")
    val avatar: String?,
    @SerializedName("online")
    val online: Boolean?,
    @SerializedName("store_id")
    val storeId: String?
) : Serializable
