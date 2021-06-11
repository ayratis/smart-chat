package gb.smartchat.library.ui.chat_profile.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatProfileDocBinding
import gb.smartchat.library.data.download.DownloadStatus
import gb.smartchat.library.entity.File
import gb.smartchat.library.utils.visible

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

        when (file.downloadStatus) {
            is DownloadStatus.Empty -> {
                binding.progressBarFile.visible(false)
                binding.ivIcon.setImageResource(R.drawable.ic_download_40)
            }
            is DownloadStatus.Downloading -> {
                binding.progressBarFile.visible(true)
                binding.ivIcon.setImageResource(R.drawable.ic_doc_40)
            }
            is DownloadStatus.Success -> {
                binding.progressBarFile.visible(false)
                binding.ivIcon.setImageResource(R.drawable.ic_doc_40)
            }
        }
    }
}
