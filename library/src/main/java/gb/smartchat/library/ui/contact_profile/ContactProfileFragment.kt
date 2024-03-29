package gb.smartchat.library.ui.contact_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.ui._global.viewbinding.FragmentContactProfileBinding
import gb.smartchat.library.ui.chat_profile.files.ChatProfileFilesFragment
import gb.smartchat.library.ui.chat_profile.links.ChatProfileLinksFragment
import gb.smartchat.library.utils.addSystemTopPadding
import gb.smartchat.library.utils.registerOnBackPress
import gb.smartchat.library.utils.visible

class ContactProfileFragment : Fragment() {

    companion object {
        private const val ARG_CONTACT = "arg contact"
        private const val ARG_CHAT_ID = "arg chat id"

        fun create(contact: Contact, chatId: Long?) = ContactProfileFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_CONTACT, contact)
                chatId?.let { putLong(ARG_CHAT_ID, it) }
            }
        }
    }

    private var _binding: FragmentContactProfileBinding? = null
    private val binding: FragmentContactProfileBinding
        get() = _binding!!

    private val contact by lazy {
        requireArguments().getSerializable(ARG_CONTACT) as Contact
    }

    private val chatId: Long by lazy {
        requireArguments().getLong(ARG_CHAT_ID, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactProfileBinding.inflate(inflater, container, false)
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
            .load(contact.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .circleCrop()
            .into(binding.ivPhoto)
        binding.tvName.text = contact.name
        binding.tvOnline.text = getString(
            if (contact.online == true) R.string.online
            else R.string.offline
        )
        binding.viewPager.adapter = ViewPageAdapter()
        binding.tabLayout.setupWithViewPager(binding.viewPager)

        binding.divider1.visible(chatId > 0)
        binding.tabLayout.visible(chatId > 0)
        binding.divider2.visible(chatId > 0)
    }

    inner class ViewPageAdapter : FragmentPagerAdapter(childFragmentManager) {
        override fun getCount(): Int {
            return if (chatId > 0) 3 else 0
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when(position) {
                0 -> getString(R.string.media)
                1 -> getString(R.string.links)
                2 -> getString(R.string.documents)
                else -> throw RuntimeException()
            }
        }

        override fun getItem(position: Int): Fragment {
            return when(position) {
                0 -> ChatProfileFilesFragment.create(chatId, true, contact.id)
                1 -> ChatProfileLinksFragment.create(chatId, contact.id)
                2 -> ChatProfileFilesFragment.create(chatId, false, contact.id)
                else -> throw RuntimeException()
            }
        }

    }
}
