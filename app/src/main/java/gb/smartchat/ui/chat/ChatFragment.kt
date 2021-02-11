package gb.smartchat.ui.chat

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatBinding
import gb.smartchat.di.InstanceFactory
import gb.smartchat.ui.chat.state_machine.Store
import gb.smartchat.utils.addSystemBottomPadding
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.visible
import io.reactivex.disposables.CompositeDisposable

class ChatFragment : Fragment(R.layout.fragment_chat) {

    companion object {
        private const val TAG = "ChatFragment"
        private const val ARG_USER_ID = "arg_user_id"
        private const val ARG_CHAT_ID = "arg_chat_id"

        fun create(userId: String, chatId: Long) = ChatFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USER_ID, userId)
                putLong(ARG_CHAT_ID, chatId)
            }
        }
    }

    private val argUserId: String by lazy {
        requireArguments().getString(ARG_USER_ID)!!
    }
    private val argChatId: Long by lazy {
        requireArguments().getLong(ARG_CHAT_ID)
    }
    private val viewModel by viewModels<ChatViewModel> {
        ChatViewModel.Factory(
            store = Store(argUserId),
            userId = argUserId,
            chatId = argChatId,
            socketApi = InstanceFactory.createSocketApi(argUserId)
        )
    }
    private val binding by viewBinding(FragmentChatBinding::bind)
    private val compositeDisposable = CompositeDisposable()

    private val chatAdapter by lazy {
        ChatAdapter(
            onItemBindListener = { chatItem ->
                viewModel.onChatItemBind(chatItem)
            },
            onDeleteListener = { chatItem ->
                viewModel.onDeleteMessage(chatItem.message)
            },
            onEditListener = { chatItem ->
                viewModel.onEditMessageRequest(chatItem.message)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.spacerTop.addSystemTopPadding()
        binding.spacerBottom.addSystemBottomPadding()
        binding.rvChat.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
        binding.etInput.doAfterTextChanged {
            viewModel.onTextChanged(it?.toString() ?: "")
        }
        binding.btnMainAction.setOnClickListener {
            viewModel.onSendClick()
        }
        binding.btnEditingClose.setOnClickListener {
            viewModel.onEditMessageReject()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
        compositeDisposable.addAll(
            viewModel.viewState
                .map { it.chatItems }
                .distinctUntilChanged()
                .subscribe { chatItems ->
                    chatAdapter.submitList(chatItems) {
                        if (chatItems.lastOrNull() is ChatItem.Outgoing) {
                            binding.rvChat.scrollToPosition(chatItems.lastIndex)
                        }
                    }
                },
            viewModel.viewState
                .map { listOf(it.editingMessage) }
                .distinctUntilChanged()
                .subscribe {
                    val editingMessage = it.first()
                    binding.viewEditingMessage.visible(editingMessage != null)
                    binding.tvEditingMessage.text = editingMessage?.text
                    binding.btnMainAction.text =
                        if (editingMessage != null) getString(R.string.edit)
                        else getString(R.string.send)
                },
            viewModel.viewState
                .map { it.typingSenderIds }
                .distinctUntilChanged()
                .subscribe { typingSenderIds ->
                    binding.toolbar.subtitle =
                        if (typingSenderIds.isEmpty()) ""
                        else typingSenderIds.toString()
                },
            viewModel.setInputText.subscribe { singleEvent ->
                singleEvent.getContentIfNotHandled()?.let {
                    binding.etInput.setText(it)
                    binding.etInput.setSelection(it.length)
                }
            }
        )
    }
}