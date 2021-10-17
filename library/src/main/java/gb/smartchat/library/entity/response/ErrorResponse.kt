package gb.smartchat.library.entity.response

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error")
    val error: String?,
    @SerializedName("error_code")
    val errorCode: Int = 0
)
