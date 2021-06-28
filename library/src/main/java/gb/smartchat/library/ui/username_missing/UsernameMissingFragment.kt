package gb.smartchat.library.ui.username_missing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui._global.viewbinding.FragmentUsernameMissingBinding
import gb.smartchat.library.ui.chat_list.ChatListFragment
import gb.smartchat.library.utils.*
import io.reactivex.disposables.CompositeDisposable

class UsernameMissingFragment : Fragment() {

    companion object {
        private const val TAG = "UsernameMissingFragment"
    }

    private var _binding: FragmentUsernameMissingBinding? = null
    private val binding: FragmentUsernameMissingBinding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: UsernameMissingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return UsernameMissingViewModel(component.httpApi, component.resourceManager) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsernameMissingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerOnBackPress {
            activity?.finish()
        }

        binding.toolbar.apply {
            addSystemTopPadding()
            setNavigationOnClickListener {
                activity?.finish()
            }
        }

        binding.scrollView.addSystemBottomPadding()

        binding.etName.addTextChangedListener { editable ->
            binding.btnContinue.isEnabled = !editable?.toString().isNullOrBlank()
        }

        binding.btnContinue.setOnClickListener {
            binding.etName.text?.toString()?.let(viewModel::onContinueClicked)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.navToChats
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    parentFragmentManager.newRootScreen(
                        ChatListFragment.create(false),
                        NavAnim.OPEN
                    )
                }
            }
            .also(compositeDisposable::add)

        viewModel.showErrorDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { message ->
                    MessageDialogFragment
                        .create(message = message)
                        .show(parentFragmentManager, null)
                }
            }
            .also(compositeDisposable::add)
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}
