package gb.smartchat.library.ui.chat_profile.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui._global.viewbinding.FragmentChatProfilePageBinding
import gb.smartchat.library.utils.addSystemBottomPadding
import io.reactivex.disposables.CompositeDisposable

class ChatProfileMembersFragment : Fragment() {

    companion object {
        private const val ARG_CHAT = "arg chat"
        private const val ARG_IS_CREATOR = "arg is creator"

        fun create(chat: Chat, isCreator: Boolean) = ChatProfileMembersFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_CHAT, chat)
                putBoolean(ARG_IS_CREATOR, isCreator)
            }
        }
    }

    interface Router {
        fun navigateToContactProfile(contact: Contact)
    }

    private var _binding: FragmentChatProfilePageBinding? = null
    private val binding: FragmentChatProfilePageBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val chat: Chat by lazy {
        requireArguments().getSerializable(ARG_CHAT) as Chat
    }

    private val isCreator by lazy {
        requireArguments().getBoolean(ARG_IS_CREATOR)
    }

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: ChatProfileMembersViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ChatProfileMembersViewModel(
                    chat,
                    component.httpApi,
                    component.resourceManager,
                    component.addRecipientsPublisher,
                    component.chatEditedPublisher
                ) as T
            }
        }
    }

    private val listAdapter by lazy {
        ChatProfileMembersAdapter(
            onContactClickListener = this::navigateToContactProfile,
            onErrorActionClickListener = viewModel::onErrorActionClick,
            deleteContactListener = if (isCreator) viewModel::onDeleteUserClick else null
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatProfilePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.viewState
            .subscribe { listAdapter.submitList(it) }
            .also { compositeDisposable.add(it) }

        viewModel.showErrorDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { message ->
                    MessageDialogFragment
                        .create(message = message)
                        .show(childFragmentManager, null)
                }
            }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun navigateToContactProfile(contact: Contact) {
        (parentFragment as? Router)?.navigateToContactProfile(contact)
    }
}
