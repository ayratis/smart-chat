package gb.smartchat.ui.chat.view_holder

import android.util.Log
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMsgIncomingBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.dp
import gb.smartchat.utils.inflate
import gb.smartchat.utils.visible
import java.text.SimpleDateFormat
import java.util.*

class IncomingViewHolder private constructor(
    itemView: View,
    private val onQuoteListener: (ChatItem) -> Unit,
    private val onQuotedMsgClickListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    companion object {
        private const val TAG = "IncomingViewHolder"

        fun create(
            parent: ViewGroup,
            onQuoteListener: (ChatItem) -> Unit,
            onQuotedMsgClickListener: (ChatItem) -> Unit
        ) =
            IncomingViewHolder(
                parent.inflate(R.layout.item_chat_msg_incoming),
                onQuoteListener,
                onQuotedMsgClickListener
            )
    }

    private val sdf = SimpleDateFormat("h:mm", Locale.getDefault())
    private val binding by viewBinding(ItemChatMsgIncomingBinding::bind)
    private lateinit var chatItem: ChatItem

    init {
        binding.root.setOnCreateContextMenuListener(this)
        binding.viewQuotedMessage.setOnClickListener {
            onQuotedMsgClickListener.invoke(chatItem)
        }
    }

    fun bind(chatItem: ChatItem.Incoming) {
        this.chatItem = chatItem
        //        binding.tvContent.text = chatItem.message.id.toString() //debug
        //        return
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
            }
        } else {
            binding.viewDocAttachment.visible(false)
            binding.ivAttachmentPhoto.visible(false)
        }
        binding.viewQuotedMessage.visible(chatItem.message.quotedMessage != null)
        binding.tvQuotedMessage.text = chatItem.message.quotedMessage?.text
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
        inflater.inflate(R.menu.quote, menu)
        menu?.forEach {
            it.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
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
