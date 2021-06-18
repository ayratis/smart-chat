package gb.smartchat.library.ui.chat_profile.links

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.library.ui._global.view_holder.ErrorViewHolder
import gb.smartchat.library.ui._global.view_holder.ProgressViewHolder

class ChatProfileLinksAdapter(
    private val onLinkClickListener: (String) -> Unit,
    private val onErrorActionClickListener: (tag: String) -> Unit,
    private val loadMoreCallback: () -> Unit
) : ListAdapter<ChatProfileLinkItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatProfileLinkItem.Data -> 1
            is ChatProfileLinkItem.Error -> 2
            is ChatProfileLinkItem.Progress -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ChatProfileLinkViewHolder.create(parent, onLinkClickListener)
            2 -> ErrorViewHolder.create(parent, onErrorActionClickListener)
            3 -> ProgressViewHolder.create(parent)
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatProfileLinkItem.Data -> (holder as ChatProfileLinkViewHolder).bind(item.link)
            is ChatProfileLinkItem.Progress -> {
            }
            is ChatProfileLinkItem.Error -> (holder as ErrorViewHolder).bind(
                item.message,
                item.action,
                item.tag
            )
        }
        if (itemCount - position < 10) {
            loadMoreCallback.invoke()
        }
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ChatProfileLinkItem>() {
        override fun areItemsTheSame(
            oldItem: ChatProfileLinkItem,
            newItem: ChatProfileLinkItem
        ): Boolean {
            if (oldItem is ChatProfileLinkItem.Data && newItem is ChatProfileLinkItem.Data) {
                return oldItem.link == newItem.link
            }
            if (oldItem is ChatProfileLinkItem.Error && newItem is ChatProfileLinkItem.Error) {
                return true
            }
            if (oldItem is ChatProfileLinkItem.Progress && newItem is ChatProfileLinkItem.Progress) {
                return true
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: ChatProfileLinkItem,
            newItem: ChatProfileLinkItem
        ): Boolean {
            if (oldItem is ChatProfileLinkItem.Data && newItem is ChatProfileLinkItem.Data) {
                return oldItem.link == newItem.link
            }
            if (oldItem is ChatProfileLinkItem.Error && newItem is ChatProfileLinkItem.Error) {
                return oldItem == newItem
            }
            if (oldItem is ChatProfileLinkItem.Progress && newItem is ChatProfileLinkItem.Progress) {
                return true
            }
            return false
        }
    }
}
