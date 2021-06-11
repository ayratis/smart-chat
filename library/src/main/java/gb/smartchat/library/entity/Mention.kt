package gb.smartchat.library.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Mention(
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("offset")
    val offsetUtf8: Int,
    @SerializedName("length")
    val lengthUtf8: Int
) : Serializable
