package gb.smartchat.library.ui.select_store_info

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.ui._global.view_holder.StoreInfoViewHolder

class StoreInfoListAdapter(
    private val storeInfoList: List<StoreInfo>,
    private val onClickListener: (StoreInfo) -> Unit
) : RecyclerView.Adapter<StoreInfoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreInfoViewHolder {
        return StoreInfoViewHolder.create(parent, onClickListener)
    }

    override fun onBindViewHolder(holder: StoreInfoViewHolder, position: Int) {
        holder.bind(storeInfoList[position])
    }

    override fun getItemCount(): Int {
        return storeInfoList.size
    }
}
