package gb.smartchat.library.ui._global.view_holder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.databinding.ItemContactGroupBinding

class HeaderViewHolder private constructor(
    private val binding: ItemContactGroupBinding
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup) =
            HeaderViewHolder(
                ItemContactGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
    }

    fun bind(name: String) {
        binding.root.text = name
    }
}
