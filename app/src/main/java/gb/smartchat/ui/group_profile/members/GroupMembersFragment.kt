package gb.smartchat.ui.group_profile.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentGroupMembersBinding
import gb.smartchat.utils.addSystemBottomPadding
import io.reactivex.disposables.CompositeDisposable

class GroupMembersFragment : Fragment() {

    companion object {
        private const val ARG_CHAT_ID = "arg chat id"
        fun create(chatId: Long) = GroupMembersFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_CHAT_ID, chatId)
            }
        }
    }

    private var _binding: FragmentGroupMembersBinding? = null
    private val binding: FragmentGroupMembersBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val chatId: Long by lazy {
        requireArguments().getLong(ARG_CHAT_ID)
    }

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: GroupMembersViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return GroupMembersViewModel(
                    chatId,
                    component.httpApi,
                    component.resourceManager
                ) as T
            }
        }
    }

    private val listAdapter by lazy {
        GroupMembersAdapter(
            onContactClickListener = viewModel::onContactClick,
            onErrorActionClickListener = viewModel::onErrorActionClick
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupMembersBinding.inflate(inflater, container, false)
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
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}
