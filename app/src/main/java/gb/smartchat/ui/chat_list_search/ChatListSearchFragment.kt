package gb.smartchat.ui.chat_list_search

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
import gb.smartchat.Component
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentChatListSearchBinding
import gb.smartchat.utils.*
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
                    ChatListSearchUDF.Store()
                ) as T
            }
        }
    }

    private val searchResultsAdapter by lazy {
        SearchResultsAdapter(
            userId = component.userId,
            onChatClickListener = {},
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
        viewModel.viewState
            .subscribe {
                searchResultsAdapter.fullData =
                    it.pagingState == ChatListSearchUDF.PagingState.FULL_DATA
                searchResultsAdapter.submitList(it.chatList)
            }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}
