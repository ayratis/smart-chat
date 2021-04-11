package gb.smartchat.entity.response


import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.Group

data class ContactListResponse(
    @SerializedName("groups")
    val groups: List<Group>?
)