package gb.smartchat.entity

import com.google.gson.annotations.SerializedName

data class Mention(
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("offset")
    val offsetUtf8: Int,
    @SerializedName("length")
    val lengthUtf8: Int
)
