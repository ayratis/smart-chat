package gb.smartchat.library.entity

import java.io.Serializable

data class StoreInfo(
    val storeId: String?,
    val storeName: String?,
    val partnerCode: Int?,
    val partnerName: String?,
    val agentCode: Int?,
    val partnerAvatar: String?
): Serializable
