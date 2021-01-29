package gb.smartchat.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import gb.smartchat.ui.global.view_holder.DummyViewHolder

class ChatAdapter : ListAdapter<ChatItem, DummyViewHolder>(ChatItem.DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DummyViewHolder {
        return DummyViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: DummyViewHolder, position: Int) {
        holder.bind(getItem(position).id.toString())
    }

}