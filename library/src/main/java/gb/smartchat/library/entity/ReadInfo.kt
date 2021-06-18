package gb.smartchat.library.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ReadInfo(
    @SerializedName("read_in")
    val readIn: Long,
    @SerializedName("read_out")
    val readOut: Long,
    @SerializedName("count")
    val unreadCount: Int
) : Serializable
