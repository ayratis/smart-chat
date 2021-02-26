package gb.smartchat.ui.chat.view_holder

import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
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
import java.text.SimpleDateFormat
import java.util.*

class OutgoingViewHolder private constructor(
    itemView: View,
    private val onDeleteListener: (ChatItem) -> Unit,
    private val onEditListener: (ChatItem) -> Unit,
    private val onQuoteListener: (ChatItem) -> Unit,
    private val onQuotedMsgClickListener: (ChatItem) -> Unit,
    private val onFileClickListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    companion object {
        private const val TAG = "DummyViewHolder"

        fun create(
            parent: ViewGroup,
            onDeleteListener: (ChatItem) -> Unit,
            onEditListener: (ChatItem) -> Unit,
            onQuoteListener: (ChatItem) -> Unit,
            onQuotedMsgClickListener: (ChatItem) -> Unit,
            onFileClickListener: (ChatItem) -> Unit
        ) =
            OutgoingViewHolder(
                parent.inflate(R.layout.item_chat_msg_outgoing),
                onDeleteListener,
                onEditListener,
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener
            )
    }

    private val binding by viewBinding(ItemChatMsgOutgoingBinding::bind)
    private lateinit var chatItem: ChatItem
    private val sdf = SimpleDateFormat("h:mm", Locale.getDefault())

    init {
        binding.root.setOnCreateContextMenuListener(this)
        binding.viewQuotedMessage.setOnClickListener {
            onQuotedMsgClickListener.invoke(chatItem)
        }
        binding.viewDocAttachment.setOnClickListener {
            onFileClickListener.invoke(chatItem)
        }
    }

    fun bind(chatItem: ChatItem.Outgoing) {
        this.chatItem = chatItem
//        binding.tvContent.text = chatItem.message.id.toString() //debug
//        return
//        val statusString = when (chatItem.status) {
//            ChatItem.OutgoingStatus.SENDING -> "`"
//            ChatItem.OutgoingStatus.SENT -> "+"
//            ChatItem.OutgoingStatus.SENT_2 -> "++"
//            ChatItem.OutgoingStatus.READ -> "+++"
//            ChatItem.OutgoingStatus.FAILURE -> "error"
//            ChatItem.OutgoingStatus.EDITING -> "editing"
//            ChatItem.OutgoingStatus.DELETING -> "deleting"
//            ChatItem.OutgoingStatus.DELETED -> "deleted"
//        }
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
                when(chatItem.message.file.downloadStatus) {
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
                ChatItem.OutgoingStatus.SENT_2 -> R.drawable.ic_double_check_12
                ChatItem.OutgoingStatus.READ -> R.drawable.ic_double_check_colored_12
                else -> R.drawable.ic_one_check_12
            }
        )

        binding.tvContent.text = chatItem.message.text
        binding.tvContent.visible(!chatItem.message.text.isNullOrBlank())
        binding.tvTime.text = chatItem.message.timeCreated?.let { sdf.format(it) }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val inflater = MenuInflater(itemView.context)
        inflater.inflate(R.menu.outgoing_message, menu)
        inflater.inflate(R.menu.quote, menu)
        menu?.forEach {
            it.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        Log.d(TAG, "onCreateContextMenu: delete")
                        onDeleteListener.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_edit -> {
                        Log.d(TAG, "onCreateContextMenu: edit")
                        onEditListener.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_quote -> {
                        Log.d(TAG, "onCreateContextMenu: quote")
                        onQuoteListener.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        }
    }
}
