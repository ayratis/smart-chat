package gb.smartchat.ui.user_profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.FragmentUserProfileBinding
import gb.smartchat.entity.UserProfile
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.registerOnBackPress

class UserProfileFragment : Fragment() {

    companion object {
        private const val ARG_USER_PROFILE = "arg user profile"

        fun create(userProfile: UserProfile) = UserProfileFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_USER_PROFILE, userProfile)
            }
        }
    }

    private var _binding: FragmentUserProfileBinding? = null
    private val binding: FragmentUserProfileBinding
        get() = _binding!!

    private val userProfile: UserProfile by lazy {
        requireArguments().getSerializable(ARG_USER_PROFILE) as UserProfile
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
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
        userProfile.avatar?.let {
            Glide.with(binding.ivAvatar)
                .load(it)
                .placeholder(R.drawable.profile_avatar_placeholder)
                .circleCrop()
                .into(binding.ivAvatar)
        }
        binding.tvName.text = userProfile.name
        binding.tvPosition.text = userProfile.description
        binding.tvPhone.apply {
            text = userProfile.phone
            setOnClickListener {
                userProfile.phone?.let {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$it")
                    }
                    if (intent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(intent)
                    }
                }
            }
        }
    }
}
