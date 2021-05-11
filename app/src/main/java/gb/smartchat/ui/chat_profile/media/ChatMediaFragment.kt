package gb.smartchat.ui.chat_profile.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentChatMediaBinding
import gb.smartchat.utils.addSystemBottomPadding
import io.reactivex.disposables.CompositeDisposable

class ChatMediaFragment : Fragment() {

    companion object {
        private const val ARG_CHAT_ID = "arg chat id"
        fun create(chatId: Long) = ChatMediaFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_CHAT_ID, chatId)
            }
        }
    }

    private var _binding: FragmentChatMediaBinding? = null
    private val binding: FragmentChatMediaBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val chatId: Long by lazy {
        requireArguments().getLong(ARG_CHAT_ID)
    }

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: ChatMediaViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ChatMediaViewModel(
                    chatId,
                    component.httpApi,
                    component.resourceManager,
                    ChatMediaUDF.Store()
                ) as T
            }
        }
    }

    private val listAdapter by lazy {
        ChatMediaAdapter(
            onFileClickListener = viewModel::onFileClick,
            onErrorActionClickListener = viewModel::onErrorActionClick,
            loadMoreCallback = viewModel::loadMore
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatMediaBinding.inflate(inflater, container, false)
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
            layoutManager = GridLayoutManager(context, 4).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (listAdapter.getItemViewType(position) == 1) 1 else 4
                    }
                }
            }
            adapter = listAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.listItems
            .subscribe { listAdapter.submitList(it) }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}
