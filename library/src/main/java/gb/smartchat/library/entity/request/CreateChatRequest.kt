package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Contact

data class CreateChatRequest(
    @SerializedName("chat_name")
    val chatName: String,
    @SerializedName("file_url")
    val fileUrl: String?,
    @SerializedName("contacts")
    val contacts: List<Contact>,

    @SerializedName("store_id")
    val storeId: String?,
    @SerializedName("store_name")
    val storeName: String?,
    @SerializedName("partner_code")
    val partnerCode: Int?,
    @SerializedName("partner_name")
    val partnerName: String?,
    @SerializedName("agent_code")
    val agentCode: Int?,
)
