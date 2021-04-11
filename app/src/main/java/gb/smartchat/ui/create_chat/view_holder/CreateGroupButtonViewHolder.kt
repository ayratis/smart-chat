package gb.smartchat.ui.create_chat.view_holder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.databinding.ItemContactBinding

class CreateGroupButtonViewHolder private constructor(
    binding: ItemContactBinding,
    private val clickListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup, clickListener: () -> Unit) =
            CreateGroupButtonViewHolder(
                ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                clickListener
            )
    }

    init {
        binding.root.setOnClickListener {
            clickListener.invoke()
        }
        binding.ivAvatar.setImageResource(R.drawable.group_avatar_placeholder)
        binding.tvName.text = itemView.context.getString(R.string.create_group)
    }
}
