package gb.smartchat.ui.chat_list

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatListBinding
import gb.smartchat.ui.SmartChatActivity
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding: FragmentChatListBinding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }
    private val chatListAdapter by lazy {
        ChatListAdapter(component.userId) { chat ->
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
                return ChatListViewModel(
                    ChatListUDF.Store(),
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
        binding.toolbar.setNavigationOnClickListener {
            activity?.finish()
        }
        binding.rvChatList.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = chatListAdapter
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            binding.btnCreateChat.doOnApplyWindowInsets { _, insets, _ ->
                binding.btnCreateChat.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    bottomMargin = 16.dp(binding.btnCreateChat) + insets.systemWindowInsetBottom
                }
                insets
            }
        }

        binding.btnCreateChat.setOnClickListener {

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
