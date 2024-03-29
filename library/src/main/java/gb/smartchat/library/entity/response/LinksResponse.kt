package gb.smartchat.library.entity.response

import com.google.gson.annotations.SerializedName

data class LinksResponse(
    @SerializedName("links")
    val links: List<String>
)
