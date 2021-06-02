package gb.smartchat.ui.chat_profile.files

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.File
import gb.smartchat.ui._global.view_holder.ErrorViewHolder
import gb.smartchat.ui._global.view_holder.ProgressViewHolder

class ChatProfileFilesAdapter(
    private val onFileClickListener: (File) -> Unit,
    private val onMediaClickListener: (File) -> Unit,
    private val onErrorActionClickListener: (tag: String) -> Unit,
    private val loadMoreCallback: () -> Unit
) : ListAdapter<ChatProfileFileItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatProfileFileItem.Media -> 1
            is ChatProfileFileItem.Error -> 2
            is ChatProfileFileItem.Progress -> 3
            is ChatProfileFileItem.Doc -> 4
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ChatProfileMediaViewHolder.create(parent, onMediaClickListener)
            2 -> ErrorViewHolder.create(parent, onErrorActionClickListener)
            3 -> ProgressViewHolder.create(parent)
            4 -> ChatProfileDocViewHolder.create(parent, onFileClickListener)
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatProfileFileItem.Media -> (holder as ChatProfileMediaViewHolder).bind(item.file)
            is ChatProfileFileItem.Doc -> (holder as ChatProfileDocViewHolder).bind(item.file)
            is ChatProfileFileItem.Progress -> {
            }
            is ChatProfileFileItem.Error -> (holder as ErrorViewHolder).bind(
                item.message,
                item.action,
                item.tag
            )
        }
        if (itemCount - position < 10) {
            loadMoreCallback.invoke()
        }
    }

    fun isSingleSpan(position: Int) : Boolean {
        return getItem(position) !is ChatProfileFileItem.Media
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ChatProfileFileItem>() {
        override fun areItemsTheSame(
            oldItem: ChatProfileFileItem,
            newItem: ChatProfileFileItem
        ): Boolean {
            if (oldItem is ChatProfileFileItem.Media && newItem is ChatProfileFileItem.Media) {
                return oldItem.file.url == newItem.file.url
            }
            if (oldItem is ChatProfileFileItem.Doc && newItem is ChatProfileFileItem.Doc) {
                return oldItem.file.url == newItem.file.url
            }
            if (oldItem is ChatProfileFileItem.Error && newItem is ChatProfileFileItem.Error) {
                return true
            }
            if (oldItem is ChatProfileFileItem.Progress && newItem is ChatProfileFileItem.Progress) {
                return true
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: ChatProfileFileItem,
            newItem: ChatProfileFileItem
        ): Boolean {
            if (oldItem is ChatProfileFileItem.Media && newItem is ChatProfileFileItem.Media) {
                return oldItem.file == newItem.file
            }
            if (oldItem is ChatProfileFileItem.Doc && newItem is ChatProfileFileItem.Doc) {
                return oldItem.file == newItem.file
            }
            if (oldItem is ChatProfileFileItem.Error && newItem is ChatProfileFileItem.Error) {
                return oldItem == newItem
            }
            if (oldItem is ChatProfileFileItem.Progress && newItem is ChatProfileFileItem.Progress) {
                return true
            }
            return false
        }
    }
}
