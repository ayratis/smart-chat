package gb.smartchat.library.ui.user_profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.FragmentUserProfileBinding
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.UserProfile
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui.chat.AttachDialogFragment
import gb.smartchat.library.utils.addSystemTopPadding
import gb.smartchat.library.utils.registerOnBackPress
import gb.smartchat.library.utils.visible
import io.reactivex.disposables.CompositeDisposable

class UserProfileFragment : Fragment(), AttachDialogFragment.Listener {

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

    private val compositeDisposable = CompositeDisposable()

    private val userProfile: UserProfile by lazy {
        requireArguments().getSerializable(ARG_USER_PROFILE) as UserProfile
    }

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: UserProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return UserProfileViewModel(
                    component.httpApi,
                    component.contentHelper,
                    component.userAvatarChangedPublisher,
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
        binding.layoutAvatar.setOnClickListener {
            AttachDialogFragment
                .create(camera = true, gallery = true, files = false)
                .show(childFragmentManager, null)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.avatarState
            .subscribe { avatarState ->
                when (avatarState) {
                    is AvatarState.Empty -> {
                        Glide
                            .with(binding.ivPhoto)
                            .load(userProfile.avatar)
                            .placeholder(R.drawable.profile_avatar_placeholder)
                            .circleCrop()
                            .into(binding.ivPhoto)
                        binding.progressBarPhoto.visible(false)
                    }
                    is AvatarState.Uploading -> {
                        Glide
                            .with(binding.ivPhoto)
                            .load(avatarState.uri)
                            .placeholder(R.drawable.profile_avatar_placeholder)
                            .circleCrop()
                            .into(binding.ivPhoto)
                        binding.progressBarPhoto.visible(true)
                    }
                    is AvatarState.UploadSuccess -> {
                        Glide
                            .with(binding.ivPhoto)
                            .load(avatarState.uri)
                            .placeholder(R.drawable.profile_avatar_placeholder)
                            .circleCrop()
                            .into(binding.ivPhoto)
                        binding.progressBarPhoto.visible(false)
                    }
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

    override fun onPhotoFromGallery(uri: Uri) {
        viewModel.uploadAvatar(uri)
    }

    override fun onCameraPicture(uri: Uri) {
        viewModel.uploadAvatar(uri)
    }
}
