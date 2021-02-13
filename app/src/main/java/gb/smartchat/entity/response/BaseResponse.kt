package gb.smartchat.entity.response

import com.google.gson.annotations.SerializedName

data class BaseResponse<T>(
    @SerializedName("result")
    val result: T
)