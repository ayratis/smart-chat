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

class ContactViewHolder private constructor(
    itemView: View,
    private val clickListener: (Contact) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    companion object {
        fun create(parent: ViewGroup, clickListener: (Contact) -> Unit) =
            ContactViewHolder(
                parent.inflate(R.layout.item_chat_contact),
                clickListener
            )
    }

    private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
    private val ivAction: ImageView = itemView.findViewById(R.id.iv_action)
    private val tvName: TextView = itemView.findViewById(R.id.tv_name)

    private lateinit var contact: Contact

    init {
        ivAction.setImageResource(R.drawable.ic_kb_right_24)
        itemView.setOnClickListener {
            clickListener.invoke(contact)
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
