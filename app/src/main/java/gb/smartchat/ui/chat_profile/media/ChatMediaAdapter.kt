package gb.smartchat.ui.chat_profile.media

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.File
import gb.smartchat.ui._global.view_holder.ErrorViewHolder
import gb.smartchat.ui._global.view_holder.ProgressViewHolder

class ChatMediaAdapter(
    private val onFileClickListener: (File) -> Unit,
    private val onErrorActionClickListener: (tag: String) -> Unit,
    private val loadMoreCallback: () -> Unit
) : ListAdapter<MediaItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MediaItem.Data -> 1
            is MediaItem.Error -> 2
            is MediaItem.Progress -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ChatMediaViewHolder.create(parent, onFileClickListener)
            2 -> ErrorViewHolder.create(parent, onErrorActionClickListener)
            3 -> ProgressViewHolder.create(parent)
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MediaItem.Data -> (holder as ChatMediaViewHolder).bind(item.file)
            is MediaItem.Progress -> {
            }
            is MediaItem.Error -> (holder as ErrorViewHolder).bind(
                item.message,
                item.action,
                item.tag
            )
        }
        if (itemCount - position < 10) {
            loadMoreCallback.invoke()
        }
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(
            oldItem: MediaItem,
            newItem: MediaItem
        ): Boolean {
            if (oldItem is MediaItem.Data && newItem is MediaItem.Data) {
                return oldItem.file.id == newItem.file.id
            }
            if (oldItem is MediaItem.Error && newItem is MediaItem.Error) {
                return true
            }
            if (oldItem is MediaItem.Progress && newItem is MediaItem.Progress) {
                return true
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: MediaItem,
            newItem: MediaItem
        ): Boolean {
            if (oldItem is MediaItem.Data && newItem is MediaItem.Data) {
                return oldItem.file == newItem.file
            }
            if (oldItem is MediaItem.Error && newItem is MediaItem.Error) {
                return oldItem == newItem
            }
            if (oldItem is MediaItem.Progress && newItem is MediaItem.Progress) {
                return true
            }
            return false
        }

    }
}
