package gb.smartchat.library.ui._global.view_holder.create_chat

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.library.utils.inflate

class CreateGroupButtonViewHolder private constructor(
    itemView: View,
    private val clickListener: () -> Unit
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(parent: ViewGroup, clickListener: () -> Unit) =
            CreateGroupButtonViewHolder(
                parent.inflate(R.layout.item_chat_contact),
                clickListener
            )
    }

    private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
    private val tvName: TextView = itemView.findViewById(R.id.tv_name)

    init {
        itemView.setOnClickListener {
            clickListener.invoke()
        }
        ivAvatar.setImageResource(R.drawable.group_avatar_placeholder)
        tvName.text = itemView.context.getString(R.string.create_group)
    }
}
