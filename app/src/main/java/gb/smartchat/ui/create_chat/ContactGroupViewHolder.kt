package gb.smartchat.ui.create_chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.databinding.ItemContactGroupBinding
import gb.smartchat.entity.Group

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
