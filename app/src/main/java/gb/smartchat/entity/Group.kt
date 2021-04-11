package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

data class Group(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("contacts")
    val contacts: List<Contact>
)