package gb.smartchat.library.ui._global.view_holder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.library.ui._global.viewbinding.ItemErrorBinding
import gb.smartchat.library.utils.visible

class ErrorViewHolder private constructor(
    private val binding: ItemErrorBinding,
    private val actionClickListener: (tag: String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup, actionClickListener: (tag: String) -> Unit) =
            ErrorViewHolder(
                ItemErrorBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                actionClickListener
            )
    }

    private var tag: String? = null

    init {
        binding.btnAction.setOnClickListener {
            tag?.let(actionClickListener)
        }
    }

    fun bind(message: String?, action: String?, tag: String?) {
        this.tag = tag
        binding.tvMessage.text = message
        binding.btnAction.apply {
            text = action
            visible(!action.isNullOrBlank())
        }
    }
}
