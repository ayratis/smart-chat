package gb.smartchat.ui.group_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.FragmentGroupProfileBinding
import gb.smartchat.entity.Chat
import gb.smartchat.ui.group_profile.members.GroupMembersFragment
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.registerOnBackPress

class GroupProfileFragment : Fragment() {

    companion object {
        private const val ARG_CHAT = "arg chat"

        fun create(chat: Chat) = GroupProfileFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_CHAT, chat)
            }
        }
    }

    private var _binding: FragmentGroupProfileBinding? = null
    private val binding: FragmentGroupProfileBinding
        get() = _binding!!

    private val chat: Chat by lazy {
        requireArguments().getSerializable(ARG_CHAT) as Chat
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupProfileBinding.inflate(inflater, container, false)
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

    }

    inner class ViewPageAdapter : FragmentPagerAdapter(childFragmentManager) {
        override fun getCount(): Int {
            return 1
        }

        override fun getItem(position: Int): Fragment {
            return GroupMembersFragment.create(chat.id)
        }

    }
}
