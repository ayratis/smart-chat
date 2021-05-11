package gb.smartchat.ui.chat_profile.links

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.databinding.ItemChatProfileLinkBinding

class ChatProfileLinkViewHolder private constructor(
    private val binding: ItemChatProfileLinkBinding,
    onClickListener: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup, onClickListener: (String) -> Unit) =
            ChatProfileLinkViewHolder(
                ItemChatProfileLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onClickListener
            )
    }

    private lateinit var link: String

    init {
        binding.root.setOnClickListener {
            onClickListener.invoke(link)
        }
    }

    fun bind(link: String) {
        this.link = link
        binding.tvLink.text = link
    }
}
