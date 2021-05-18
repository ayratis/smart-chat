package gb.smartchat.entity

import java.io.Serializable

data class StoreInfo(
    val storeId: String,
    val storeName: String,
    val partnerCode: Int,
    val partnerName: String,
    val agentCode: Int,
): Serializable {
    companion object {
        fun fake(): StoreInfo = StoreInfo(
            "asd",
            "asd",
            123,
            "asd",
            123,
        )
    }
}
