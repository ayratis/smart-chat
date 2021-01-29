package gb.smartchat.ui.global.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.ItemDummyBinding
import gb.smartchat.utils.inflate

class DummyViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding by viewBinding(ItemDummyBinding::bind)

    fun bind(text: String) {
        binding.tvContent.text = text
    }

    companion object {
        fun create(parent: ViewGroup) = DummyViewHolder(parent.inflate(R.layout.item_dummy))
    }
}