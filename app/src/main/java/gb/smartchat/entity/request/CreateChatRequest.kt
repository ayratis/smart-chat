package gb.smartchat.entity.request

import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.Contact

data class CreateChatRequest(
    @SerializedName("store_id")
    val storeId: String,
    @SerializedName("store_name")
    val storeName: String,
    @SerializedName("partner_code")
    val partnerCode: Int,
    @SerializedName("partner_name")
    val partnerName: String,
    @SerializedName("agent_code")
    val agentCode: Int,
    @SerializedName("contacts")
    val contacts: List<Contact>
)
