package gb.smartchat.library.ui._global.view_holder.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.drawable.Drawable
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
import gb.smartchat.library.data.download.DownloadStatus
import gb.smartchat.library.entity.File
import gb.smartchat.library.entity.Mention
import gb.smartchat.library.entity.Message
import gb.smartchat.library.ui._global.viewbinding.ItemChatMsgOutgoingBinding
import gb.smartchat.library.ui.chat.ChatItem
import gb.smartchat.library.utils.*
import java.time.format.DateTimeFormatter


class OutgoingViewHolder private constructor(
    itemView: View,
    private val onDeleteListener: ((ChatItem.Msg) -> Unit)?,
    private val onEditListener: ((ChatItem.Msg) -> Unit)?,
    private val onQuoteListener: ((ChatItem.Msg) -> Unit)?,
    private val onQuotedMsgClickListener: ((ChatItem.Msg) -> Unit)?,
    private val onFileClickListener: (ChatItem.Msg) -> Unit,
    private val onMentionClickListener: ((Mention) -> Unit)?,
    private val onToFavoritesClickListener: ((ChatItem.Msg) -> Unit)?,
    private val onPhotoClickListener: (File) -> Unit,
    private val onShareListener: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "OutgoingViewHolder"

        fun create(
            parent: ViewGroup,
            onDeleteListener: ((ChatItem.Msg) -> Unit)?,
            onEditListener: ((ChatItem.Msg) -> Unit)?,
            onQuoteListener: ((ChatItem.Msg) -> Unit)?,
            onQuotedMsgClickListener: ((ChatItem.Msg) -> Unit)?,
            onFileClickListener: (ChatItem.Msg) -> Unit,
            onMentionClickListener: ((Mention) -> Unit)?,
            onToFavoritesClickListener: ((ChatItem.Msg) -> Unit)?,
            onPhotoClickListener: (File) -> Unit,
            onShareListener: (String) -> Unit,
        ) =
            OutgoingViewHolder(
                parent.inflate(R.layout.item_chat_msg_outgoing),
                onDeleteListener,
                onEditListener,
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener,
                onMentionClickListener,
                onToFavoritesClickListener,
                onPhotoClickListener,
                onShareListener
            )
    }

    private val binding = ItemChatMsgOutgoingBinding.bind(itemView)
    private val imgIcon: Drawable by lazy {
        binding.root.context.drawable(R.drawable.ic_img_14).apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
    private val docIcon: Drawable by lazy {
        binding.root.context.drawable(R.drawable.ic_doc_14).apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
    private lateinit var chatItem: ChatItem.Msg
    private val sdf = DateTimeFormatter.ofPattern("H:mm")
    private val clipboard by lazy {
        itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

    init {
        binding.content.setOnClickListener { showMenu() }
        binding.content.setOnLongClickListener(this::onLongClickListener)
        binding.viewQuotedMessage.setOnClickListener {
            onQuotedMsgClickListener?.invoke(chatItem)
        }
        binding.viewQuotedMessage.setOnLongClickListener(this::onLongClickListener)
        binding.viewDocAttachment.setOnClickListener {
            onFileClickListener.invoke(chatItem)
        }
        binding.viewDocAttachment.setOnLongClickListener(this::onLongClickListener)
        binding.ivAttachmentPhoto.setOnClickListener {
            chatItem.message.file?.let(onPhotoClickListener::invoke)
        }
        binding.ivAttachmentPhoto.setOnLongClickListener(this::onLongClickListener)
        binding.tvContent.setOnLongClickListener(this::onLongClickListener)
        binding.tvContent.setOnClickListener { showMenu() }
    }

    private fun onLongClickListener(view: View): Boolean {
        showMenu()
        return true
    }

    fun bind(chatItem: ChatItem.Msg.Outgoing) {
        this.chatItem = chatItem
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
        binding.tvQuotedPerson.text = chatItem.message.quotedMessage?.user?.name
        val icon: Drawable? = when {
            chatItem.message.quotedMessage?.file == null -> null
            chatItem.message.quotedMessage.file.isImage() -> imgIcon
            else -> docIcon
        }
        binding.tvQuotedMessage.setCompoundDrawables(icon, null, null, null)
        binding.tvQuotedMessage.text = when {
            !chatItem.message.quotedMessage?.text.isNullOrBlank() ->
                chatItem.message.quotedMessage?.text
            chatItem.message.quotedMessage?.file?.isImage() == true ->
                binding.root.context.getString(R.string.photo)
            chatItem.message.quotedMessage?.file != null ->
                chatItem.message.quotedMessage.file.name
            else -> null
        }
        binding.ivStatus.visible(chatItem.message.type != Message.Type.DELETED)
        binding.ivStatus.setImageResource(
            when (chatItem.status) {
                ChatItem.OutgoingStatus.SENT -> R.drawable.ic_double_check_12
                ChatItem.OutgoingStatus.RED -> R.drawable.ic_double_check_colored_12
            }
        )

        binding.tvContent.apply {
            if (chatItem.message.mentions.isNullOrEmpty()) {
                text = chatItem.message.text
            } else {
                text = SpannableStringBuilder(chatItem.message.text).apply {
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
                                onMentionClickListener?.invoke(mention)
                            },
                            offset,
                            offset + mentionString.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
            movementMethod = LinkMovementMethod.getInstance()
            visible(!chatItem.message.text.isNullOrBlank())
        }
        binding.tvTime.text = sdf.format(chatItem.message.timeCreated)
        binding.tvEdited.visible(
            chatItem.message.timeUpdated != null &&
                    chatItem.message.type != Message.Type.DELETED
        )
    }

    private fun showMenu() {
        if (chatItem.message.type == Message.Type.DELETED) return

        val menu = android.widget.PopupMenu(itemView.context, binding.content)
        if (onQuoteListener != null) {
            menu.inflate(R.menu.quote)
        }
        menu.inflate(R.menu.copy)
        menu.inflate(R.menu.share)
        if (chatItem.message.file?.downloadStatus == DownloadStatus.Empty) {
            menu.inflate(R.menu.download)
        }
        if (onEditListener != null) {
            menu.inflate(R.menu.edit)
        }
        if (onToFavoritesClickListener != null) {
            menu.inflate(R.menu.to_favorites)
        }
        if (onDeleteListener != null) {
            menu.inflate(R.menu.delete)
        }

        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_quote -> {
                    Log.d(TAG, "onCreateContextMenu: quote")
                    onQuoteListener?.invoke(chatItem)
                    true
                }
                R.id.action_copy -> {
                    Log.d(TAG, "onCreateContextMenu: copy")
                    val clipText = listOfNotNull(
                        chatItem.message.text,
                        chatItem.message.file?.url
                    ).joinToString("\n")
                    val clip = ClipData.newPlainText(null, clipText)
                    clipboard.setPrimaryClip(clip)
                    true
                }
                R.id.action_share -> {
                    Log.d(TAG, "onCreateContextMenu: share")
                    val shareText = listOfNotNull(
                        chatItem.message.text,
                        chatItem.message.file?.url
                    ).joinToString("\n")
                    onShareListener.invoke(shareText)
                    true
                }
                R.id.action_download -> {
                    Log.d(TAG, "onCreateContextMenu: download")
                    if (chatItem.message.file?.downloadStatus == DownloadStatus.Empty) {
                        onFileClickListener.invoke(chatItem)
                    }
                    true
                }
                R.id.action_edit -> {
                    Log.d(TAG, "onCreateContextMenu: edit")
                    onEditListener?.invoke(chatItem)
                    true
                }
                R.id.action_delete -> {
                    Log.d(TAG, "onCreateContextMenu: delete")
                    onDeleteListener?.invoke(chatItem)
                    true
                }
                R.id.action_to_favorites -> {
                    Log.d(TAG, "onCreateContextMenu: to_favorites")
                    onToFavoritesClickListener?.invoke(chatItem)
                    true
                }
                else -> false
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            menu.setForceShowIcon(true)
        }
        menu.show()
    }
}
