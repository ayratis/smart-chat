
package gb.smartchat.ui.chat

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatBinding
import gb.smartchat.utils.addSystemBottomPadding
import gb.smartchat.utils.addSystemTopPadding

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private val binding by viewBinding(FragmentChatBinding::bind)
    private val viewModel by viewModels<ChatViewModel>()

    private val chatAdapter by lazy {
        ChatAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.spacerTop.addSystemTopPadding()
        binding.spacerBottom.addSystemBottomPadding()
        binding.rvChat.apply {
            setHasFixedSize(true)
            addSystemBottomPadding()
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
        binding.btnSend.setOnClickListener {
            binding.etInput.text.toString().let {
                if (it.isNotBlank()) viewModel.onSendClick(it)
                binding.etInput.setText("")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
        viewModel.chatList.observe(this) { list ->
            chatAdapter.submitList(list) {
                binding.rvChat.scrollToPosition(list.lastIndex)
            }
        }
    }
}