package gb.smartchat.ui.chat_list

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
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentChatListBinding
import gb.smartchat.entity.Chat
import gb.smartchat.ui._global.MessageDialogFragment
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.ui.chat_list_search.ChatListSearchFragment
import gb.smartchat.ui.create_chat.CreateChatFragment
import gb.smartchat.ui.create_chat.CreateChatMode
import gb.smartchat.ui.user_profile.UserProfileFragment
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable

class ChatListFragment : Fragment(), MessageDialogFragment.OnClickListener {

    companion object {
        private const val PROFILE_ERROR_TAG = "profile error tag"
        private const val PIN_ERROR_TAG = "pin error tag"
        private const val ARG_IS_ARCHIVE = "arg is archive"

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
                    component.leaveChatPublisher
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
                        else -> false
                    }
                }
            }
            binding.btnCreateChat.apply {
                visible(true)
                setOnClickListener {
                    profileViewModel.onCreateChatClick()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
                            binding.toolbar.title = null
                            binding.profileContent.visible(false)
                        }
                        is ChatListProfileUDF.State.Empty,
                        is ChatListProfileUDF.State.Loading -> {
                            binding.toolbar.title = getString(R.string.refreshing)
                            binding.profileContent.visible(false)
                        }
                        is ChatListProfileUDF.State.Success -> {
                            binding.toolbar.title = null
                            binding.profileContent.visible(true)
                            Glide.with(binding.ivProfileAvatar)
                                .load(profileState.userProfile.avatar)
                                .placeholder(R.drawable.profile_avatar_placeholder)
                                .circleCrop()
                                .into(binding.ivProfileAvatar)
                            binding.tvProfileName.text = profileState.userProfile.name
                            binding.profileContent.setOnClickListener {
                                parentFragmentManager.navigateTo(
                                    UserProfileFragment.create(profileState.userProfile),
                                    NavAnim.SLIDE
                                )
                            }
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
                    event.getContentIfNotHandled()?.let { (storeInfo, userProfile) ->
                        parentFragmentManager.navigateTo(
                            CreateChatFragment.create(
                                storeInfo,
                                CreateChatMode.CREATE_SINGLE,
                                userProfile
                            ),
                            NavAnim.OPEN
                        )
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
            ChatFragment.create(chat),
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
}
