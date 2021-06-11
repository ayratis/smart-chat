package gb.smartchat.library.entity.response


import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Group

data class ContactListResponse(
    @SerializedName("groups")
    val groups: List<Group>?
)
