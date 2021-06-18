package gb.smartchat.library.ui.chat.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import gb.smartchat.R
import gb.smartchat.databinding.ItemMentionBinding
import gb.smartchat.library.entity.User
import gb.smartchat.library.utils.dp
import gb.smartchat.library.utils.inflate
import gb.smartchat.library.utils.visible

class MentionViewHolder private constructor(
    itemView: View,
    clickListener: (User) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "MentionViewHolder"

        fun create(parent: ViewGroup, clickListener: (User) -> Unit) =
            MentionViewHolder(parent.inflate(R.layout.item_mention), clickListener)
    }

    private val binding = ItemMentionBinding.bind(itemView)
    private lateinit var user: User

    init {
        binding.root.setOnClickListener {
            clickListener.invoke(user)
        }
    }

    fun bind(user: User, isFirst: Boolean, isLast: Boolean) {
        this.user = user
        binding.dividerTop.visible(isFirst)
        Glide.with(binding.ivPhoto)
            .load(user.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .transform(
                CenterCrop(),
                RoundedCorners(12.dp(binding.ivPhoto))
            )
            .into(binding.ivPhoto)
        binding.tvName.text = user.name
        binding.dividerBot.visible(!isLast)
    }
}
