package gb.smartchat.library.ui.chat_profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui._global.viewbinding.FragmentChatProfileBinding
import gb.smartchat.library.ui.chat.AttachDialogFragment
import gb.smartchat.library.ui.chat_profile.files.ChatProfileFilesFragment
import gb.smartchat.library.ui.chat_profile.links.ChatProfileLinksFragment
import gb.smartchat.library.ui.chat_profile.members.ChatProfileMembersFragment
import gb.smartchat.library.ui.contact_profile.ContactProfileFragment
import gb.smartchat.library.ui.create_chat.CreateChatFragment
import gb.smartchat.library.ui.create_chat.CreateChatMode
import gb.smartchat.library.utils.*
import io.reactivex.disposables.CompositeDisposable

class ChatProfileFragment : Fragment(),
    ChatProfileMembersFragment.Router,
    MessageDialogFragment.OnClickListener,
    AttachDialogFragment.Listener {

    companion object {
        private const val ARG_CHAT = "arg chat"
        private const val LEAVE_CHAT_DIALOG_TAG = "leave chat dialog tag"
        private const val ARCHIVE_CHAT_DIALOG_TAG = "archive chat dialog tag"
        private const val UNARCHIVE_CHAT_DIALOG_TAG = "unarchive chat dialog tag"

        fun create(chat: Chat) = ChatProfileFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_CHAT, chat)
            }
        }
    }

    private var _binding: FragmentChatProfileBinding? = null
    private val binding: FragmentChatProfileBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val chat: Chat by lazy {
        requireArguments().getSerializable(ARG_CHAT) as Chat
    }

    private val viewModel: ChatProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ChatProfileViewModel(
                    component.httpApi,
                    chat,
                    component.userId,
                    component.resourceManager,
                    component.leaveChatPublisher,
                    component.chatArchivePublisher,
                    component.chatUnarchivePublisher,
                    component.contentHelper,
                    component.chatEditedPublisher
                ) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatProfileBinding.inflate(inflater, container, false)
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

        binding.ivPhoto.setOnClickListener {
            AttachDialogFragment
                .create(camera = true, gallery = true, files = false)
                .show(childFragmentManager, null)
        }
        binding.tvGroupName.text = chat.name
        binding.tvMemberCount.text = getString(R.string.d_members, chat.users.size)
        Glide.with(binding.ivPartnerIcon)
            .load(chat.partnerAvatar)
            .into(binding.ivPartnerIcon)
        binding.ivPartnerIcon.visible(chat.partnerAvatar != null)
        binding.tvAgentName.text = chat.storeName
        binding.viewPager.adapter = ViewPageAdapter()
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.btnAddMembers.apply {
            visible(chat.isArchived != true)
            setOnClickListener {
                parentFragmentManager.navigateTo(
                    CreateChatFragment.create(
                        storeInfo = chat.storeInfo,
                        mode = CreateChatMode.ADD_MEMBERS,
                        chat = chat
                    )
                )
            }
        }
        binding.btnArchive.apply {

            val drawableIcon = context.drawable(
                if (chat.isArchived == true) R.drawable.ic_group_profile_unarchive_chat
                else R.drawable.ic_group_profile_archive_chat
            )

            setCompoundDrawablesWithIntrinsicBounds(null, drawableIcon, null, null)

            text = getString(
                if (chat.isArchived == true) R.string.unarchive
                else R.string.archive
            )

            setOnClickListener {
                if (chat.isArchived == true) {
                    MessageDialogFragment
                        .create(
                            message = getString(R.string.unarchive_chat_dialog_message),
                            positive = getString(R.string.yes),
                            negative = getString(R.string.no),
                            tag = UNARCHIVE_CHAT_DIALOG_TAG
                        )
                        .show(childFragmentManager, UNARCHIVE_CHAT_DIALOG_TAG)
                } else {
                    MessageDialogFragment
                        .create(
                            message = getString(R.string.archive_chat_dialog_message),
                            positive = getString(R.string.yes),
                            negative = getString(R.string.no),
                            tag = ARCHIVE_CHAT_DIALOG_TAG
                        )
                        .show(childFragmentManager, ARCHIVE_CHAT_DIALOG_TAG)
                }
            }
        }

        binding.btnLeaveChat.setOnClickListener {
            MessageDialogFragment
                .create(
                    message = getString(R.string.leave_chat_dialog_message),
                    positive = getString(R.string.yes),
                    negative = getString(R.string.no),
                    tag = LEAVE_CHAT_DIALOG_TAG
                )
                .show(childFragmentManager, LEAVE_CHAT_DIALOG_TAG)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.avatar
            .subscribe { uri ->
                Glide.with(binding.ivPhoto)
                    .load(uri)
                    .placeholder(R.drawable.group_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivPhoto)
            }
            .also { compositeDisposable.add(it) }

        viewModel.photoUploading
            .subscribe { binding.progressBarPhoto.visible(it) }
            .also { compositeDisposable.add(it) }

        viewModel.exitToRootScreen
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    parentFragmentManager.backTo(null)
                }
            }
            .also { compositeDisposable.add(it) }
        viewModel.showDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { message ->
                    MessageDialogFragment.create(message = message)
                        .show(childFragmentManager, null)
                }
            }
            .also { compositeDisposable.add(it) }


    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun navigateToContactProfile(contact: Contact) {
        parentFragmentManager.navigateTo(
            ContactProfileFragment.create(contact, chat.id),
            NavAnim.SLIDE
        )
    }

    override fun dialogPositiveClicked(tag: String) {
        when (tag) {
            LEAVE_CHAT_DIALOG_TAG -> viewModel.leaveChat()
            ARCHIVE_CHAT_DIALOG_TAG -> viewModel.archiveChat()
            UNARCHIVE_CHAT_DIALOG_TAG -> viewModel.unarchiveChat()
        }
    }

    override fun onPhotoFromGallery(uri: Uri) {
        viewModel.editPhoto(uri)
    }

    override fun onCameraPicture(uri: Uri) {
        viewModel.editPhoto(uri)
    }

    inner class ViewPageAdapter : FragmentPagerAdapter(childFragmentManager) {
        override fun getCount(): Int {
            return 4
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> getString(R.string.members)
                1 -> getString(R.string.media)
                2 -> getString(R.string.links)
                3 -> getString(R.string.documents)
                else -> throw RuntimeException()
            }
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> {
                    val isCreator = chat.isAdmin == true && chat.isArchived == false
                    ChatProfileMembersFragment.create(chat.id, isCreator)
                }
                1 -> ChatProfileFilesFragment.create(chat.id, true)
                2 -> ChatProfileLinksFragment.create(chat.id)
                3 -> ChatProfileFilesFragment.create(chat.id, false)
                else -> throw RuntimeException()
            }
        }
    }
}
