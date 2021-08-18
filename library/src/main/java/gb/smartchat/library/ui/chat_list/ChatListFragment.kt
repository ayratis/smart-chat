package gb.smartchat.library.ui.chat_list

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui._global.ProgressDialog
import gb.smartchat.library.ui._global.viewbinding.FragmentChatListBinding
import gb.smartchat.library.ui.chat.ChatFragment
import gb.smartchat.library.ui.chat_list_search.ChatListSearchFragment
import gb.smartchat.library.ui.create_chat.CreateChatFragment
import gb.smartchat.library.ui.create_chat.CreateChatMode
import gb.smartchat.library.ui.select_store_info.SelectStoreInfoFragment
import gb.smartchat.library.ui.user_profile.UserProfileFragment
import gb.smartchat.library.utils.*
import io.reactivex.disposables.CompositeDisposable

class ChatListFragment : Fragment(), MessageDialogFragment.OnClickListener {

    companion object {
        private const val PROFILE_ERROR_TAG = "profile error tag"
        private const val PIN_ERROR_TAG = "pin error tag"
        private const val ARG_IS_ARCHIVE = "arg is archive"
        private const val PROGRESS_TAG = "progress_tag"

        fun create(isArchive: Boolean) = ChatListFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_ARCHIVE, isArchive)
            }
        }
    }

    private var _binding: FragmentChatListBinding? = null
    private val binding: FragmentChatListBinding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private val argIsArchive by lazy {
        requireArguments().getBoolean(ARG_IS_ARCHIVE)
    }
    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }
    private val chatListAdapter by lazy {
        ChatListAdapter(
            userId = component.userId,
            isArchive = argIsArchive,
            clickListener = this::navigateToChat,
            pinListener = viewModel::onPinChatClick,
            archiveListener = viewModel::onArchiveChatClick,
            nextPageCallback = viewModel::loadMore
        )
    }
    private val viewModel: ChatListViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ChatListViewModel(
                    argIsArchive,
                    ChatListUDF.Store(component.userId),
                    component.httpApi,
                    component.socketApi,
                    component.resourceManager,
                    component.chatCreatedPublisher,
                    component.messageReadInternalPublisher,
                    component.chatUnreadMessageCountPublisher,
                    component.chatUnarchivePublisher,
                    component.leaveChatPublisher,
                    component.chatArchivePublisher,
                    component.chatEditedPublisher
                ) as T
            }
        }
    }

    private val profileViewModel: ChatListProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ChatListProfileViewModel(
                    component.httpApi,
                    ChatListProfileUDF.Store(),
                    component.resourceManager,
                    component.userAvatarChangedPublisher
                ) as T
            }
        }
    }

    private val favoriteMessagesViewModel: FavoriteMessagesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return FavoriteMessagesViewModel(
                    component.httpApi,
                    component.resourceManager
                ) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBarLayout.addSystemTopPadding()
        registerOnBackPress {
            if (argIsArchive) {
                parentFragmentManager.popBackStack()
            } else {
                activity?.finish()
            }
        }

        binding.rvChatList.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = chatListAdapter
        }
        if (argIsArchive) {
            binding.profileContent.visible(false)
            binding.toolbar.apply {
                setTitleTextColor(context.color(R.color.black))
                title = getString(R.string.archive_messages)
                setNavigationOnClickListener {
                    parentFragmentManager.popBackStack()
                }
            }
            binding.btnCreateChat.visible(false)
        } else {
            binding.toolbar.apply {
                setTitleTextColor(context.color(R.color.gray))
                setNavigationOnClickListener {
                    activity?.finish()
                }
                inflateMenu(R.menu.search)
                inflateMenu(R.menu.archive_messages)
                inflateMenu(R.menu.favorite_messages)
                inflateMenu(R.menu.user_profile)
                inflateMenu(R.menu.close_chats)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.search -> {
                            dismissPopupMenus()
                            parentFragmentManager.navigateTo(
                                ChatListSearchFragment(),
                                NavAnim.OPEN
                            )
                            true
                        }
                        R.id.action_archive_messages -> {
                            dismissPopupMenus()
                            parentFragmentManager.navigateTo(
                                create(true),
                                NavAnim.SLIDE
                            )
                            true
                        }
                        R.id.action_favorite_messages -> {
                            dismissPopupMenus()
                            favoriteMessagesViewModel.onFavoriteMessagesClick()
                            true
                        }
                        R.id.action_user_profile -> {
                            dismissPopupMenus()
                            navigateToUserProfile()
                            true
                        }
                        R.id.action_close_chats -> {
                            dismissPopupMenus()
                            activity?.finish()
                            true
                        }
                        else -> false
                    }
                }
            }
            binding.profileContent.setOnClickListener {
                navigateToUserProfile()
            }
            binding.btnCreateChat.apply {
                visible(true)
                setOnClickListener {
                    profileViewModel.onCreateChatClick()
                }
                val isTablet = resources.getBoolean(R.bool.tablet)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !isTablet) {
                    doOnApplyWindowInsets { _, insets, _ ->
                        updateLayoutParams<CoordinatorLayout.LayoutParams> {
                            bottomMargin = 16.dp(this@apply) + insets.systemWindowInsetBottom
                        }
                        insets
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!argIsArchive) {
            profileViewModel.viewState
                .subscribe { profileState ->
                    when (profileState) {
                        is ChatListProfileUDF.State.Error -> {
                            component.userProfile = null
                            binding.toolbar.title = null
                            binding.profileContent.visible(false)
                        }
                        is ChatListProfileUDF.State.Empty,
                        is ChatListProfileUDF.State.Loading -> {
                            component.userProfile = null
                            binding.toolbar.title = getString(R.string.refreshing)
                            binding.profileContent.visible(false)
                        }
                        is ChatListProfileUDF.State.Success -> {
                            component.userProfile = profileState.userProfile
                            binding.toolbar.title = null
                            binding.profileContent.visible(true)
                            Glide.with(binding.ivProfileAvatar)
                                .load(profileState.userProfile.avatar)
                                .placeholder(R.drawable.profile_avatar_placeholder)
                                .circleCrop()
                                .into(binding.ivProfileAvatar)
                            binding.tvProfileName.text = profileState.userProfile.name
                        }
                    }
                }
                .also { compositeDisposable.add(it) }

            profileViewModel.showProfileErrorDialog
                .subscribe { event ->
                    event.getContentIfNotHandled()?.let {
                        MessageDialogFragment
                            .create(message = it, tag = PROFILE_ERROR_TAG)
                            .show(childFragmentManager, PROFILE_ERROR_TAG)
                    }
                }
                .also { compositeDisposable.add(it) }

            profileViewModel.navToCreateChat
                .subscribe { event ->
                    event.getContentIfNotHandled()?.let {
                        val storeInfoList = component.storeInfoList
                        when {
                            storeInfoList.size == 1 -> {
                                parentFragmentManager.navigateTo(
                                    CreateChatFragment.create(
                                        storeInfoList.first(),
                                        CreateChatMode.CREATE_GROUP
                                    ),
                                    NavAnim.OPEN
                                )
                            }
                            storeInfoList.size > 1 -> {
                                parentFragmentManager.navigateTo(
                                    SelectStoreInfoFragment(),
                                    NavAnim.OPEN
                                )
                            }
                        }
                    }
                }
                .also { compositeDisposable.add(it) }

            favoriteMessagesViewModel.fullScreenProgress
                .subscribe { showProgressDialog(it) }
                .also { compositeDisposable.add(it) }

            favoriteMessagesViewModel.navToFavoriteChat
                .subscribe { event ->
                    event.getContentIfNotHandled()?.let {
                        parentFragmentManager.navigateTo(
                            ChatFragment.create(it.id, it, isFavoritesChat = true),
                            NavAnim.SLIDE
                        )
                    }
                }
                .also { compositeDisposable.add(it) }

            favoriteMessagesViewModel.showMessageDialog
                .subscribe { event ->
                    event.getContentIfNotHandled()?.let {
                        MessageDialogFragment
                            .create(message = it)
                            .show(childFragmentManager, null)
                    }
                }
                .also { compositeDisposable.add(it) }
        }

        viewModel.viewState
            .subscribe {
                chatListAdapter.submitList(it.chatList)
                chatListAdapter.fullData = it.pagingState == ChatListUDF.PagingState.FULL_DATA
            }
            .also { compositeDisposable.add(it) }

        viewModel.showErrorMessage
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { message ->
                    MessageDialogFragment
                        .create(message = message, tag = PIN_ERROR_TAG)
                        .show(childFragmentManager, PIN_ERROR_TAG)
                }
            }
            .also { compositeDisposable.add(it) }
    }

    private fun navigateToChat(chat: Chat) {
        parentFragmentManager.navigateTo(
            ChatFragment.create(chat.id, chat),
            NavAnim.SLIDE
        )
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun dialogCanceled(tag: String) {
        if (tag == PROFILE_ERROR_TAG) {
            activity?.finish()
        }
    }

    override fun dialogPositiveClicked(tag: String) {
        if (tag == PROFILE_ERROR_TAG) {
            activity?.finish()
        }
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

    private fun navigateToUserProfile() {
        val userProfile = component.userProfile ?: return
        parentFragmentManager.navigateTo(
            UserProfileFragment.create(userProfile),
            NavAnim.SLIDE
        )
    }
}
