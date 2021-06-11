package gb.smartchat.library.ui.chat_profile.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.databinding.ItemChatProfileMediaBinding
import gb.smartchat.library.entity.File

class ChatProfileMediaViewHolder private constructor(
    private val binding: ItemChatProfileMediaBinding,
    onClickListener: (File) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup, onClickListener: (File) -> Unit) =
            ChatProfileMediaViewHolder(
                ItemChatProfileMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false),
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
        Glide.with(binding.root)
            .load(file.url)
            .centerCrop()
            .into(binding.root)
    }
}
