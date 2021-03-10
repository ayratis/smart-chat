package gb.smartchat.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.User
import gb.smartchat.ui.chat.view_holder.MentionViewHolder

class MentionAdapter(
    private val clickListener: (User) -> Unit
) : RecyclerView.Adapter<MentionViewHolder>() {

    private val items = ArrayList<User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentionViewHolder {
        return MentionViewHolder.create(parent, clickListener)
    }

    override fun onBindViewHolder(holder: MentionViewHolder, position: Int) {
        holder.bind(items[position], isFirst = position == 0, isLast = position == itemCount - 1)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setData(data: List<User>) {
        val oldList = items.toList()
        items.clear()
        items.addAll(data)
        DiffUtil.calculateDiff(DiffUtilCallback(oldList, items)).dispatchUpdatesTo(this)
    }

    class DiffUtilCallback(
        private val oldList: List<User>,
        private val newList: List<User>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
