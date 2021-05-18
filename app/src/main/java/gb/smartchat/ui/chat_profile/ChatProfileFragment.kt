package gb.smartchat.ui.chat_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentChatProfileBinding
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Contact
import gb.smartchat.entity.StoreInfo
import gb.smartchat.entity.User
import gb.smartchat.ui.chat_profile.files.ChatProfileFilesFragment
import gb.smartchat.ui.chat_profile.links.ChatProfileLinksFragment
import gb.smartchat.ui.chat_profile.members.ChatProfileMembersFragment
import gb.smartchat.ui.contact_profile.ContactProfileFragment
import gb.smartchat.ui.create_chat.CreateChatFragment
import gb.smartchat.ui.create_chat.CreateChatMode
import gb.smartchat.utils.NavAnim
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.navigateTo
import gb.smartchat.utils.registerOnBackPress

class ChatProfileFragment : Fragment(), ChatProfileMembersFragment.Router {

    companion object {
        private const val ARG_CHAT = "arg chat"

        fun create(chat: Chat) = ChatProfileFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_CHAT, chat)
            }
        }
    }

    private var _binding: FragmentChatProfileBinding? = null
    private val binding: FragmentChatProfileBinding
        get() = _binding!!

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val chat: Chat by lazy {
        requireArguments().getSerializable(ARG_CHAT) as Chat
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

        Glide.with(binding.ivPhoto)
            .load(chat.avatar)
            .placeholder(R.drawable.group_avatar_placeholder)
            .circleCrop()
            .into(binding.ivPhoto)
        binding.tvGroupName.text = chat.name
        binding.tvMemberCount.text = getString(R.string.d_members, chat.users.size)
        binding.tvAgentName.text = chat.agentName
        binding.viewPager.adapter = ViewPageAdapter()
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.btnAddMembers.setOnClickListener {
            parentFragmentManager.navigateTo(
                CreateChatFragment.create(
                    storeInfo = StoreInfo.fake(),
                    mode = CreateChatMode.ADD_MEMBERS,
                    chat = chat
                )
            )
        }
    }

    override fun navigateToContactProfile(contact: Contact) {
        parentFragmentManager.navigateTo(
            ContactProfileFragment.create(contact, chat.id),
            NavAnim.SLIDE
        )
    }

    inner class ViewPageAdapter : FragmentPagerAdapter(childFragmentManager) {
        override fun getCount(): Int {
            return 4
        }

        override fun getPageTitle(position: Int): CharSequence? {
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
                    val creator = chat.users.find { it.role == User.Role.CREATOR }
                    val isCreator = creator?.id == component.userId
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
