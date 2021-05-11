package gb.smartchat.ui.chat_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatProfileBinding
import gb.smartchat.entity.Chat
import gb.smartchat.ui.chat_profile.files.ChatProfileFilesFragment
import gb.smartchat.ui.chat_profile.members.ChatProfileMembersFragment
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.registerOnBackPress

class ChatProfileFragment : Fragment() {

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

    }

    inner class ViewPageAdapter : FragmentPagerAdapter(childFragmentManager) {
        override fun getCount(): Int {
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when(position) {
                0 -> getString(R.string.members)
                1 -> getString(R.string.media)
                2 -> getString(R.string.documents)
                else -> throw RuntimeException()
            }
        }

        override fun getItem(position: Int): Fragment {
            return when(position) {
                0 -> ChatProfileMembersFragment.create(chat.id)
                1 -> ChatProfileFilesFragment.create(chat.id, true)
                2 -> ChatProfileFilesFragment.create(chat.id, false)
                else -> throw RuntimeException()
            }
        }

    }
}
