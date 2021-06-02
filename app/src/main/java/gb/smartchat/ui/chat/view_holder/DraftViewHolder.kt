package gb.smartchat.ui.chat.view_holder

import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import gb.smartchat.R
import gb.smartchat.data.download.DownloadStatus
import gb.smartchat.databinding.ItemChatMsgOutgoingBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.dp
import gb.smartchat.utils.inflate
import gb.smartchat.utils.visible
import java.time.format.DateTimeFormatter
import java.util.*

class DraftViewHolder private constructor(
    itemView: View,
    private val onDeleteListener: ((ChatItem.Msg) -> Unit)?,
    private val onEditListener: ((ChatItem.Msg) -> Unit)?,
    private val onQuoteListener: ((ChatItem.Msg) -> Unit)?,
    private val onQuotedMsgClickListener: ((ChatItem.Msg) -> Unit)?,
    private val onFileClickListener: (ChatItem.Msg) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    companion object {
        private const val TAG = "DraftViewHolder"

        fun create(
            parent: ViewGroup,
            onDeleteListener: ((ChatItem.Msg) -> Unit)?,
            onEditListener: ((ChatItem.Msg) -> Unit)?,
            onQuoteListener: ((ChatItem.Msg) -> Unit)?,
            onQuotedMsgClickListener: ((ChatItem.Msg) -> Unit)?,
            onFileClickListener: (ChatItem.Msg) -> Unit
        ) =
            DraftViewHolder(
                parent.inflate(R.layout.item_chat_msg_outgoing),
                onDeleteListener,
                onEditListener,
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener
            )
    }

    private val binding = ItemChatMsgOutgoingBinding.bind(itemView)
    private lateinit var chatItem: ChatItem.Msg
    private val sdf = DateTimeFormatter.ofPattern("H:mm")

    init {
        binding.root.setOnCreateContextMenuListener(this)
        binding.viewQuotedMessage.setOnClickListener {
            onQuotedMsgClickListener?.invoke(chatItem)
        }
        binding.viewDocAttachment.setOnClickListener {
            onFileClickListener.invoke(chatItem)
        }
    }

    fun bind(chatItem: ChatItem.Msg.Draft) {
        this.chatItem = chatItem
        if (chatItem.message.file != null) {
            val mimeType = chatItem.message.file.url?.let {
                val extension = MimeTypeMap.getFileExtensionFromUrl(it)
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
            Log.d(TAG, "bind: file mimeType: $mimeType")
            if (mimeType?.startsWith("image") == true) {
                binding.viewDocAttachment.visible(false)
                binding.ivAttachmentPhoto.visible(true)
                Glide.with(binding.ivAttachmentPhoto)
                    .load(chatItem.message.file.url)
                    .transform(
                        CenterCrop(),
                        RoundedCorners(12.dp(binding.ivAttachmentPhoto))
                    )
                    .into(binding.ivAttachmentPhoto)
            } else {
                binding.ivAttachmentPhoto.visible(false)
                binding.viewDocAttachment.visible(true)
                binding.tvDocName.text = chatItem.message.file.name
                binding.tvDocSize.text = chatItem.message.file.size?.let { "${it / 1000} KB" }
                when (chatItem.message.file.downloadStatus) {
                    is DownloadStatus.Empty -> {
                        binding.progressBarFile.visible(false)
                        binding.ivDocIcon.setImageResource(R.drawable.ic_download_40)
                    }
                    is DownloadStatus.Downloading -> {
                        binding.progressBarFile.visible(true)
                        binding.ivDocIcon.setImageResource(R.drawable.ic_doc_40)
                    }
                    is DownloadStatus.Success -> {
                        binding.progressBarFile.visible(false)
                        binding.ivDocIcon.setImageResource(R.drawable.ic_doc_40)
                    }
                }
            }
        } else {
            binding.viewDocAttachment.visible(false)
            binding.ivAttachmentPhoto.visible(false)
        }
        binding.viewQuotedMessage.visible(chatItem.message.quotedMessage != null)
        binding.tvQuotedMessage.text = chatItem.message.quotedMessage?.text
        binding.ivStatus.setImageResource(
            when (chatItem.status) {
                ChatItem.DraftStatus.FAILURE -> R.drawable.ic_time_12
                ChatItem.DraftStatus.SENDING -> R.drawable.ic_time_12
                ChatItem.DraftStatus.SENT -> R.drawable.ic_one_check_12
            }
        )

        binding.tvContent.text = chatItem.message.text
        binding.tvContent.visible(!chatItem.message.text.isNullOrBlank())
        binding.tvTime.text = sdf.format(chatItem.message.timeCreated)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val inflater = MenuInflater(itemView.context)
        if (onDeleteListener != null) {
            inflater.inflate(R.menu.delete, menu)
        }
        if (onQuoteListener != null) {
            inflater.inflate(R.menu.quote, menu)
        }
        menu?.forEach {
            it.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        Log.d(TAG, "onCreateContextMenu: delete")
                        onDeleteListener?.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_edit -> {
                        Log.d(TAG, "onCreateContextMenu: edit")
                        onEditListener?.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_quote -> {
                        Log.d(TAG, "onCreateContextMenu: quote")
                        onQuoteListener?.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        }
    }
}
