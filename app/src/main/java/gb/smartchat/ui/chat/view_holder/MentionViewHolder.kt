package gb.smartchat.ui.chat.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import gb.smartchat.R
import gb.smartchat.databinding.ItemMentionBinding
import gb.smartchat.entity.User
import gb.smartchat.utils.dp
import gb.smartchat.utils.inflate

class MentionViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "MentionViewHolder"

        fun create(parent: ViewGroup) =
            MentionViewHolder(parent.inflate(R.layout.item_mention))
    }

    private val binding = ItemMentionBinding.bind(itemView)

    fun bind(user: User) {
        Glide.with(binding.ivPhoto)
            .load(user.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .transform(
                CenterCrop(),
                RoundedCorners(12.dp(binding.ivPhoto))
            )
            .into(binding.ivPhoto)

        binding.tvName.text = user.name
    }
}
