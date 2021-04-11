package gb.smartchat.entity

import java.io.Serializable

data class StoreInfo(
    val storeId: String,
    val storeName: String,
    val partnerCode: Int,
    val partnerName: String,
    val agentCode: Int,
    val userProfile: UserProfile
): Serializable