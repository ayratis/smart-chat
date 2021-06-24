package gb.smartchat.library.ui.select_store_info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.ui._global.viewbinding.FragmentSelectStoreInfoBinding
import gb.smartchat.library.ui.create_chat.CreateChatFragment
import gb.smartchat.library.ui.create_chat.CreateChatMode
import gb.smartchat.library.utils.*

class SelectStoreInfoFragment : Fragment() {

    private var _binding: FragmentSelectStoreInfoBinding? = null
    private val binding: FragmentSelectStoreInfoBinding get() = _binding!!

    private val component by lazy {
        (activity as SmartChatActivity).component
    }

    private val storeInfoListAdapter by lazy {
        StoreInfoListAdapter(
            storeInfoList = component.storeInfoList,
            onClickListener = this::onStoreInfoClick
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectStoreInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerOnBackPress {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.apply {
            addSystemTopPadding()
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        binding.rvStoreInfo.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = storeInfoListAdapter
        }
    }

    private fun onStoreInfoClick(storeInfo: StoreInfo) {
        parentFragmentManager.replace(
            CreateChatFragment.create(storeInfo, CreateChatMode.CREATE_GROUP),
            NavAnim.SLIDE
        )
    }
}
