
package gb.smartchat.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatBinding
import gb.smartchat.utils.addSystemBottomPadding
import gb.smartchat.utils.addSystemTopPadding

class ChatFragment : Fragment(R.layout.fragment_chat) {

    companion object {
        private const val TAG = "ChatFragment"
    }

    private val binding by viewBinding(FragmentChatBinding::bind)
    private val viewModel by viewModels<ChatViewModel>()

    private val chatAdapter by lazy {
        ChatAdapter(
            onItemBindListener = { chatItem ->
                viewModel.onChatItemBind(chatItem)
            },
            onLongClickListener = { chatItem ->

            }
        )
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
        binding.etInput.doAfterTextChanged {
            viewModel.onTextChanged(it?.toString() ?: "")
        }
        binding.btnSend.setOnClickListener {
            viewModel.onSendClick()
            binding.etInput.setText("")
        }

        registerForContextMenu(binding.rvChat)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = requireActivity().menuInflater
        inflater.inflate(R.menu.outgoing_message, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        Log.d(TAG, "onContextItemSelected: info: $info")
        when (item.itemId) {
            R.id.action_edit -> {
                Log.d(TAG, "onContextItemSelected: edit")
                return true
            }
            R.id.action_delete -> {
                Log.d(TAG, "onContextItemSelected: delete")
                return true
            }
        }
        return super.onContextItemSelected(item)
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