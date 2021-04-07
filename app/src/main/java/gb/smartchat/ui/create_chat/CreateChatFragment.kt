package gb.smartchat.ui.create_chat

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
import gb.smartchat.databinding.FragmentCreateChatBinding
import gb.smartchat.utils.addSystemBottomPadding
import gb.smartchat.utils.addSystemTopPadding
import io.reactivex.disposables.CompositeDisposable

class CreateChatFragment : Fragment() {

    private var _binding: FragmentCreateChatBinding? = null
    private val binding: FragmentCreateChatBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: CreateChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return CreateChatViewModel(
                    component.httpApi,
                    CreateChatUDF.Store(),
                ) as T
            }
        }
    }

    private val contactsAdapter by lazy {
        ContactsAdapter { contact ->

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateChatBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBarLayout.addSystemTopPadding()
        binding.rvContacts.apply {
            addSystemBottomPadding()
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.items
            .subscribe { contactsAdapter.submitList(it) }
            .also { compositeDisposable.add(it) }
    }
}
