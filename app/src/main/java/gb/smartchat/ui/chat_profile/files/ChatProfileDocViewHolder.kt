package gb.smartchat.ui.chat_profile.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.databinding.ItemChatProfileDocBinding
import gb.smartchat.entity.File

class ChatProfileDocViewHolder private constructor(
    private val binding: ItemChatProfileDocBinding,
    onClickListener: (File) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup, onClickListener: (File) -> Unit) =
            ChatProfileDocViewHolder(
                ItemChatProfileDocBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onClickListener
            )
    }

    private lateinit var file: File

    init {
        binding.root.setOnClickListener {
            onClickListener.invoke(file)
        }
    }

    fun bind(file: File) {
        this.file = file
        binding.tvName.text = file.name
        val size = "${(file.size ?: 0) / 1000} KB"
        binding.tvSizeAndDate.text = size
    }
}
