package gb.smartchat.ui.create_chat

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.R
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentCreateChatBinding
import gb.smartchat.entity.StoreInfo
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.ui.custom.MessageDialogFragment
import gb.smartchat.ui.custom.ProgressDialog
import gb.smartchat.ui.group_complete.GroupCompleteFragment
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable

class CreateChatFragment : Fragment() {

    companion object {
        private const val TAG = "CreateChatFragment"
        private const val PROGRESS_TAG = "progress tag"
        private const val ARG_STORE_INFO = "arg store info"
        private const val ARG_MODE = "arg mode"

        fun create(storeInfo: StoreInfo, mode: CreateChatMode) = CreateChatFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_STORE_INFO, storeInfo)
                putSerializable(ARG_MODE, mode)
            }
        }
    }

    private val storeInfo by lazy {
        requireArguments().getSerializable(ARG_STORE_INFO) as StoreInfo
    }
    private val mode by lazy {
        requireArguments().getSerializable(ARG_MODE) as CreateChatMode
    }
    private var _binding: FragmentCreateChatBinding? = null
    private val binding: FragmentCreateChatBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: CreateChatViewModel by simpleViewModels {
        CreateChatViewModel(
            storeInfo,
            mode,
            component.httpApi,
            CreateChatUDF.Store(mode),
            component.resourceManager,
            component.chatCreatedPublisher,
            component.contactDeletePublisher
        )
    }

    private val contactsAdapter by lazy {
        ContactsAdapter(
            createGroupClickListener = {
                parentFragmentManager.navigateTo(
                    create(storeInfo, CreateChatMode.GROUP),
                    NavAnim.SLIDE
                )
            },
            contactClickListener = viewModel::onContactClick,
            errorActionClickListener = viewModel::onErrorActionClick
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateChatBinding.inflate(inflater)
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
        binding.toolbar.apply {
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            inflateMenu(R.menu.search)
            (menu.findItem(R.id.search).actionView as SearchView).setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.onQueryTextSubmit(query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.onQueryTextChange(newText)
                        return true
                    }
                }
            )
        }
        binding.rvContacts.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }
        binding.btnCreateChat.apply {
            visible(mode == CreateChatMode.GROUP)
            setOnClickListener {
                viewModel.onCreateGroupNextClick()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
               doOnApplyWindowInsets { _, insets, _ ->
                    updateLayoutParams<CoordinatorLayout.LayoutParams> {
                        bottomMargin = 16.dp(resources) + insets.systemWindowInsetBottom
                    }
                    insets
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.items
            .subscribe { contactsAdapter.submitList(it) }
            .also { compositeDisposable.add(it) }

        if (mode == CreateChatMode.GROUP) {
            viewModel.selectedCount
                .subscribe { (selectedCount, totalCount) ->
                    binding.toolbar.subtitle = "$selectedCount / $totalCount"
                    binding.btnCreateChat.isEnabled = selectedCount > 0
                }
                .also { compositeDisposable.add(it) }

        }

        viewModel.navToChat
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    parentFragmentManager.replace(
                        ChatFragment.create(it),
                        NavAnim.SLIDE
                    )
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.showDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    MessageDialogFragment
                        .create(message = it)
                        .show(childFragmentManager, null)
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.progressDialog
            .subscribe { showProgressDialog(it) }
            .also { compositeDisposable.add(it) }

        viewModel.navToGroupComplete
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { (storeInfo, selectedContacts) ->
                    parentFragmentManager.navigateTo(
                        GroupCompleteFragment.create(storeInfo, selectedContacts),
                        NavAnim.SLIDE
                    )
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
