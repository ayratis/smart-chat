package gb.smartchat.entity

import com.google.gson.annotations.SerializedName

data class Mention(
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("length")
    val length: Int
)
