package gb.smartchat.library.entity

import java.io.Serializable

data class StoreInfo(
    val storeId: String,
    val storeName: String,
    val partnerCode: Int,
    val partnerName: String,
    val agentCode: Int,
    val partnerAvatar: String?
): Serializable {
    companion object {
        fun fake(): StoreInfo = StoreInfo(
            "7abc24fe-b7ec-11eb-8529-0242ac130003",
            "asd",
            123,
            "asd",
            123,
            null
        )
    }
}
