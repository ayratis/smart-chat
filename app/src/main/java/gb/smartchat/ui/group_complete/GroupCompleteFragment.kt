package gb.smartchat.ui.group_complete

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.Component
import gb.smartchat.R
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentGroupCompleteBinding
import gb.smartchat.entity.Contact
import gb.smartchat.entity.StoreInfo
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.ui.chat_list.ChatListFragment
import gb.smartchat.ui.custom.MessageDialogFragment
import gb.smartchat.ui.custom.ProgressDialog
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable

class GroupCompleteFragment : Fragment() {

    companion object {
        private const val TAG = "GroupCompleteFragment"
        private const val PROGRESS_TAG = "progress tag"
        private const val ARG_STORE_INFO = "arg store info"
        private const val ARG_SELECTED_CONTACTS = "arg selected contacts"

        fun create(storeInfo: StoreInfo, selectedContacts: List<Contact>) =
            GroupCompleteFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_STORE_INFO, storeInfo)
                    putSerializable(ARG_SELECTED_CONTACTS, ArrayList(selectedContacts))
                }
            }
    }

    private var _binding: FragmentGroupCompleteBinding? = null
    private val binding: FragmentGroupCompleteBinding
        get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private val storeInfo by lazy {
        requireArguments().getSerializable(ARG_STORE_INFO) as StoreInfo
    }
    private val selectedContacts: List<Contact> by lazy {
        @Suppress("UNCHECKED_CAST")
        requireArguments().getSerializable(ARG_SELECTED_CONTACTS) as ArrayList<Contact>
    }
    private val component: Component by lazy {
        (requireActivity() as SmartChatActivity).component
    }
    private val viewModel: GroupCompleteViewModel by simpleViewModels {
        GroupCompleteViewModel(
            storeInfo,
            GroupCompleteUDF.Store(selectedContacts),
            component.httpApi,
            component.resourceManager
        )
    }
    private val groupInfoAdapter by lazy {
        ContactsAdapter(viewModel::onContactDelete)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupCompleteBinding.inflate(inflater, container, false)
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

        binding.appBarLayout.addSystemTopPadding()
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.rvContent.apply {
            setHasFixedSize(true)
            isNestedScrollingEnabled = true
            addSystemBottomPadding()
            layoutManager = LinearLayoutManager(context)
            adapter = groupInfoAdapter
        }
        binding.btnCreateChat.apply {
            setOnClickListener {
                viewModel.onCreateGroup()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            binding.linearLayout.doOnApplyWindowInsets { _, insets, _ ->
                binding.rvContent.updatePadding(bottom = insets.systemWindowInsetBottom)
                binding.btnCreateChat.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    bottomMargin = 16.dp(resources) + insets.systemWindowInsetBottom
                }
                insets
            }
        }

        binding.etGroupName.doAfterTextChanged {
            viewModel.onGroupNameChanged(it?.toString() ?: "")
        }

        binding.ivPhoto.setOnClickListener {
            viewModel.onPhotoClick()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.contacts
            .subscribe {
                groupInfoAdapter.submitList(it)
                binding.tvMemberCount.text = getString(R.string.member_count_d, it.size)
            }
            .also { compositeDisposable.add(it) }

        viewModel.createEnabled
            .subscribe { binding.btnCreateChat.isEnabled = it }
            .also { compositeDisposable.add(it) }

        viewModel.progressDialog
            .subscribe { showProgressDialog(it) }
            .also { compositeDisposable.add(it) }

        viewModel.navToChat
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    parentFragmentManager.backTo(ChatListFragment::class)
                    parentFragmentManager.navigateTo(ChatFragment.create(it), NavAnim.SLIDE)
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.showDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    MessageDialogFragment.create(message = it).show(childFragmentManager, null)
                }
            }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun showProgressDialog(progress: Boolean) {
        if (!isAdded) return

        val fragment = childFragmentManager.findFragmentByTag(PROGRESS_TAG)
        if (fragment != null && !progress) {
            (fragment as ProgressDialog).dismissAllowingStateLoss()
            childFragmentManager.executePendingTransactions()
        } else if (fragment == null && progress) {
            ProgressDialog().show(childFragmentManager, PROGRESS_TAG)
            childFragmentManager.executePendingTransactions()
        }
    }
}
