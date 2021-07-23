package gb.smartchat.library.ui._global.view_holder

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.utils.inflate

class DeletableContactViewHolder private constructor(
    itemView: View,
    private val deleteClickListener: (Contact) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    companion object {
        fun create(parent: ViewGroup, deleteClickListener: (Contact) -> Unit) =
            DeletableContactViewHolder(
                parent.inflate(R.layout.item_chat_contact),
                deleteClickListener
            )
    }

    private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
    private val ivAction: ImageView = itemView.findViewById(R.id.iv_action)
    private val tvName: TextView = itemView.findViewById(R.id.tv_name)

    private lateinit var contact: Contact

    init {
        ivAction.apply {
            setImageResource(R.drawable.ic_close_red_24)
            contentDescription = itemView.context.getString(R.string.delete)
            setOnClickListener {
                deleteClickListener.invoke(contact)
            }
        }
    }

    fun bind(contact: Contact) {
        this.contact = contact
        Glide.with(ivAvatar)
            .load(contact.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .circleCrop()
            .into(ivAvatar)
        tvName.text = contact.name
    }
}
