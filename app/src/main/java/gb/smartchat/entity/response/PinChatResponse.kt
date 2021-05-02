package gb.smartchat.entity.response

import com.google.gson.annotations.SerializedName

data class PinChatResponse(
    @SerializedName("success")
    val success: Boolean
)
