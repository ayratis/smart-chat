package gb.smartchat.ui._global.view_holder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.databinding.ItemProgressBinding

class ProgressViewHolder private constructor(
    binding: ItemProgressBinding,
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup) =
            ProgressViewHolder(
                ItemProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
    }
}
