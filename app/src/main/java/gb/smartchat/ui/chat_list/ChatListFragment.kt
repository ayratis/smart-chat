package gb.smartchat.ui.chat_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatListBinding
import gb.smartchat.ui.SmartChatActivity
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.utils.addSystemBottomPadding
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.setSlideAnimation
import io.reactivex.disposables.CompositeDisposable

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding: FragmentChatListBinding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private val chatListAdapter by lazy {
        ChatListAdapter { chat ->
            parentFragmentManager
                .beginTransaction()
                .setSlideAnimation()
                .replace(R.id.fragment_container, ChatFragment.create(chat))
                .addToBackStack(ChatFragment::class.java.canonicalName)
                .commit()
            parentFragmentManager.executePendingTransactions()
        }
    }
    private val viewModel: ChatListViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                val component = (requireActivity() as SmartChatActivity).component
                return ChatListViewModel(
                    ChatListStateMachine.Store(),
                    component.httpApi,
                    component.socketApi
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
        binding.rvChatList.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = chatListAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.viewState
            .subscribe {
                chatListAdapter.submitList(it.chatList)
            }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}
