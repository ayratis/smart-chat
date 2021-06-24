package gb.smartchat.library.ui.create_chat

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.R
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui._global.ProgressDialog
import gb.smartchat.library.ui._global.viewbinding.FragmentCreateChatBinding
import gb.smartchat.library.ui.chat.ChatFragment
import gb.smartchat.library.ui.group_complete.GroupCompleteFragment
import gb.smartchat.library.utils.*
import io.reactivex.disposables.CompositeDisposable

class CreateChatFragment : Fragment() {

    companion object {
        private const val TAG = "CreateChatFragment"
        private const val PROGRESS_TAG = "progress tag"
        private const val ARG_STORE_INFO = "arg store info"
        private const val ARG_MODE = "arg mode"
        private const val ARG_CHAT = "arg chat"

        fun create(
            storeInfo: StoreInfo,
            mode: CreateChatMode,
            chat: Chat? = null
        ) =
            CreateChatFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_STORE_INFO, storeInfo)
                    putSerializable(ARG_MODE, mode)
                    chat?.let { putSerializable(ARG_CHAT, it) }
                }
            }
    }

    private val storeInfo: StoreInfo by lazy {
        requireArguments().getSerializable(ARG_STORE_INFO) as StoreInfo
    }
    private val mode by lazy {
        requireArguments().getSerializable(ARG_MODE) as CreateChatMode
    }
    private val chat: Chat? by lazy {
        requireArguments().getSerializable(ARG_CHAT) as? Chat
    }

    private var _binding: FragmentCreateChatBinding? = null
    private val binding: FragmentCreateChatBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: CreateChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return CreateChatViewModel(
                    storeInfo,
                    chat?.id,
                    component.httpApi,
                    CreateChatUDF.Store(mode),
                    component.resourceManager,
                    component.chatCreatedPublisher,
                    component.contactDeletePublisher,
                    component.addRecipientsPublisher
                ) as T
            }
        }
    }

    private val contactsAdapter by lazy {
        ContactsAdapter(
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
            title = when (mode) {
                CreateChatMode.CREATE_GROUP -> getString(R.string.create_chat)
                CreateChatMode.ADD_MEMBERS -> getString(R.string.add)
            }
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            inflateMenu(R.menu.search_with_action)
            (menu.findItem(R.id.search_with_action).actionView as SearchView).setOnQueryTextListener(
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
            when (mode) {
                CreateChatMode.CREATE_GROUP -> {
                    visible(true)
                    setImageDrawable(context.drawable(R.drawable.ic_arrow_forward_white_24))
                }
                CreateChatMode.ADD_MEMBERS -> {
                    visible(true)
                    setImageDrawable(context.drawable(R.drawable.ic_done_white_24))
                }
            }
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

        viewModel.selectedCount
            .subscribe { (selectedCount, totalCount) ->
                binding.toolbar.subtitle = "$selectedCount / $totalCount"
                binding.btnCreateChat.isEnabled = selectedCount > 0
            }
            .also { compositeDisposable.add(it) }

        viewModel.navToChat
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    parentFragmentManager.replace(
                        ChatFragment.create(it.id, it),
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
                        GroupCompleteFragment.create(
                            storeInfo = storeInfo,
                            selectedContacts = selectedContacts
                        ),
                        NavAnim.SLIDE
                    )
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.exit
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    parentFragmentManager.popBackStack()
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
