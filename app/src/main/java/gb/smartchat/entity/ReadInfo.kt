package gb.smartchat.entity

import com.google.gson.annotations.SerializedName

data class ReadInfo(
    @SerializedName("read_in")
    val readIn: Long,
    @SerializedName("read_out")
    val readOut: Long,
    @SerializedName("count")
    val unreadCount: Int
)
