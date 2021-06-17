package gb.smartchat.library.ui.chat_list_search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.databinding.FragmentChatListSearchBinding
import gb.smartchat.library.Component
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.ui.chat.ChatFragment
import gb.smartchat.library.ui.contact_profile.ContactProfileFragment
import gb.smartchat.library.utils.*
import io.reactivex.disposables.CompositeDisposable

class ChatListSearchFragment : Fragment() {

    private var _binding: FragmentChatListSearchBinding? = null
    private val binding: FragmentChatListSearchBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val component: Component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: ChatListSearchViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ChatListSearchViewModel(
                    component.httpApi,
                    ChatListSearchUDF.Store(),
                    component.resourceManager
                ) as T
            }
        }
    }

    private val searchResultsAdapter by lazy {
        SearchResultsAdapter(
            userId = component.userId,
            onChatClickListener = this::navigateToChat,
            onContactClickListener = this::navigateToContactProfile,
            nextPageCallback = viewModel::loadMore
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListSearchBinding.inflate(inflater, container, false)
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
        binding.appBarLayout.addSystemTopPadding()
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.etSearch.apply {
            showSoftInput()
            doAfterTextChanged {
                viewModel.onTextChanged(it?.toString() ?: "")
                binding.btnClearQuery.visible(!it?.toString().isNullOrEmpty())
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    viewModel.submitQuery()
                    return@setOnEditorActionListener true
                }
                false
            }
        }
        binding.btnClearQuery.setOnClickListener {
            binding.etSearch.setText("")
        }
        binding.rvSearchResults.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = searchResultsAdapter
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.fullData
            .subscribe { searchResultsAdapter.fullData = it }
            .also { compositeDisposable.add(it) }

        viewModel.items
            .subscribe { searchResultsAdapter.submitList(it) }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        hideSoftInput()
        compositeDisposable.clear()
        super.onPause()
    }

    private fun navigateToChat(chat: Chat) {
        parentFragmentManager.navigateTo(
            ChatFragment.create(chat.id, chat),
            NavAnim.SLIDE
        )
    }

    private fun navigateToContactProfile(contact: Contact) {
        parentFragmentManager.navigateTo(
            ContactProfileFragment.create(contact, chatId = null),
            NavAnim.SLIDE
        )
    }
}