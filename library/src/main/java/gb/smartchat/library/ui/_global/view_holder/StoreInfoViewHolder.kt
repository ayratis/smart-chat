package gb.smartchat.library.ui._global.view_holder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.ui._global.viewbinding.ItemStoreInfoBinding

class StoreInfoViewHolder private constructor(
    private val binding: ItemStoreInfoBinding,
    private val onClickListener: (StoreInfo) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup, onClickListener: (StoreInfo) -> Unit) =
            StoreInfoViewHolder(
                ItemStoreInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onClickListener
            )
    }

    private lateinit var storeInfo: StoreInfo

    init {
        binding.root.setOnClickListener {
            onClickListener.invoke(storeInfo)
        }
    }

    fun bind(storeInfo: StoreInfo) {
        this.storeInfo = storeInfo
        Glide
            .with(binding.ivPartnerAvatar)
            .load(storeInfo.partnerAvatar)
            .circleCrop()
            .into(binding.ivPartnerAvatar)
        binding.tvPartnerName.text = storeInfo.partnerName
        binding.tvStoreName.text = storeInfo.storeName
    }
}
