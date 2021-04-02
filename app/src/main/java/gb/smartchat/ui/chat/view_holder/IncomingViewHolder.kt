package gb.smartchat.ui.chat.view_holder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import gb.smartchat.R
import gb.smartchat.data.download.DownloadStatus
import gb.smartchat.databinding.ItemChatMsgIncomingBinding
import gb.smartchat.entity.Mention
import gb.smartchat.entity.Message
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.*
import java.time.format.DateTimeFormatter

class IncomingViewHolder private constructor(
    itemView: View,
    private val onQuoteListener: (ChatItem.Msg) -> Unit,
    private val onQuotedMsgClickListener: (ChatItem.Msg) -> Unit,
    private val onFileClickListener: (ChatItem.Msg) -> Unit,
    private val onMentionClickListener: (Mention) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "IncomingViewHolder"

        fun create(
            parent: ViewGroup,
            onQuoteListener: (ChatItem.Msg) -> Unit,
            onQuotedMsgClickListener: (ChatItem.Msg) -> Unit,
            onFileClickListener: (ChatItem.Msg) -> Unit,
            onMentionClickListener: (Mention) -> Unit,
        ) =
            IncomingViewHolder(
                parent.inflate(R.layout.item_chat_msg_incoming),
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener,
                onMentionClickListener
            )
    }

    private val sdf = DateTimeFormatter.ofPattern("H:mm")
    private val binding = ItemChatMsgIncomingBinding.bind(itemView)
    private lateinit var chatItem: ChatItem.Msg
    private val clipboard by lazy {
        itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    init {
        binding.viewQuotedMessage.setOnClickListener {
            onQuotedMsgClickListener.invoke(chatItem)
        }
        binding.viewDocAttachment.setOnClickListener {
            onFileClickListener.invoke(chatItem)
        }
        binding.content.setOnClickListener {
            if (chatItem.message.type == Message.Type.DELETED) return@setOnClickListener

            val menu = android.widget.PopupMenu(itemView.context, binding.content)
            menu.inflate(R.menu.quote)
            if (!chatItem.message.text.isNullOrBlank()) {
                menu.inflate(R.menu.copy)
            }
            if (chatItem.message.file?.downloadStatus == DownloadStatus.Empty) {
                menu.inflate(R.menu.download)
            }
            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_quote -> {
                        Log.d(TAG, "onCreateContextMenu: quote")
                        onQuoteListener.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_copy -> {
                        Log.d(TAG, "onCreateContextMenu: copy")
                        val clip = ClipData.newPlainText(null, chatItem.message.text)
                        clipboard.setPrimaryClip(clip)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_download -> {
                        Log.d(TAG, "onCreateContextMenu: download")
                        if (chatItem.message.file?.downloadStatus == DownloadStatus.Empty) {
                            onFileClickListener.invoke(chatItem)
                        }
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                menu.setForceShowIcon(true)
            }
            menu.show()
        }

    }

    fun bind(chatItem: ChatItem.Msg.Incoming) {
        this.chatItem = chatItem
        //        binding.tvContent.text = chatItem.message.id.toString() //debug
        //        return
        Glide.with(binding.ivAvatar)
            .load(chatItem.message.user?.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.tvSenderName.text = chatItem.message.user?.name
        if (chatItem.message.file != null) {
            if (chatItem.message.file.isImage()) {
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
        binding.tvContent.apply {
            text =
                if (chatItem.message.mentions.isNullOrEmpty()) {
                    chatItem.message.text
                } else {
                    SpannableStringBuilder(chatItem.message.text).apply {
                        chatItem.message.mentions.forEach { mention ->
                            val mentionUtf8 = ByteArray(mention.lengthUtf8)
                            chatItem.message.text!!.encodeToByteArray().copyInto(
                                destination = mentionUtf8,
                                startIndex = mention.offsetUtf8,
                                endIndex = mention.offsetUtf8 + mention.lengthUtf8
                            )
                            val mentionString = mentionUtf8.decodeToString()
                            val offset = chatItem.message.text.indexOf(mentionString)
                            setSpan(
                                AppClickableSpan(
                                    isUnderlineText = false,
                                    linkColor = itemView.context.color(R.color.purple_heart)
                                ) {
                                    onMentionClickListener.invoke(mention)
                                },
                                offset,
                                offset + mentionString.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }

            movementMethod =
                if (chatItem.message.mentions.isNullOrEmpty()) null
                else LinkMovementMethod.getInstance()

            visible(!chatItem.message.text.isNullOrBlank())
        }
        binding.tvTime.text = sdf.format(chatItem.message.timeCreated)
        binding.tvEdited.visible(
            chatItem.message.timeUpdated != null &&
                    chatItem.message.type != Message.Type.DELETED
        )
    }
}
