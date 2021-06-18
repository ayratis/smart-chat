package gb.smartchat.library.ui._global.view_holder.create_chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.library.entity.Group
import gb.smartchat.library.ui._global.viewbinding.ItemContactGroupBinding

class ContactGroupViewHolder private constructor(
    private val binding: ItemContactGroupBinding
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup) =
            ContactGroupViewHolder(
                ItemContactGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
    }

    fun bind(group: Group) {
        binding.root.text = group.name
    }
}
