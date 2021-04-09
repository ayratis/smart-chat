package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

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
    val phone: String?
)
