package gb.smartchat.ui.chat_list

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentChatListBinding
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.ui.create_chat.CreateChatFragment
import gb.smartchat.ui.create_chat.CreateChatMode
import gb.smartchat.ui.custom.MessageDialogFragment
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable

class ChatListFragment : Fragment(), MessageDialogFragment.OnClickListener {

    companion object {
        private const val PROFILE_ERROR_TAG = "profile error tag"
    }

    private var _binding: FragmentChatListBinding? = null
    private val binding: FragmentChatListBinding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()
    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }
    private val chatListAdapter by lazy {
        ChatListAdapter(component.userId) { chat ->
            parentFragmentManager.navigateTo(
                ChatFragment.create(chat),
                NavAnim.SLIDE
            )
            parentFragmentManager.executePendingTransactions()
        }
    }
    private val viewModel: ChatListViewModel by simpleViewModels {
        ChatListViewModel(
            ChatListUDF.Store(),
            component.httpApi,
            component.socketApi,
            component.resourceManager
        )
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
            viewModel.onCreateChatClick()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.viewState
            .subscribe {
                chatListAdapter.submitList(it.chatList)
            }
            .also { compositeDisposable.add(it) }

        viewModel.showProfileErrorDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    MessageDialogFragment
                        .create(message = it, tag = PROFILE_ERROR_TAG)
                        .show(childFragmentManager, PROFILE_ERROR_TAG)
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.navToCreateChat
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { storeInfo ->
                    parentFragmentManager.navigateTo(
                        CreateChatFragment.create(storeInfo, CreateChatMode.SINGLE),
                        NavAnim.OPEN
                    )
                }
            }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun dialogCanceled(tag: String) {
        if (tag == PROFILE_ERROR_TAG) {
            activity?.finish()
        }
    }

    override fun dialogPositiveClicked(tag: String) {
        if (tag == PROFILE_ERROR_TAG) {
            activity?.finish()
        }
    }
}
