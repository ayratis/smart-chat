package gb.smartchat.ui.create_chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import gb.smartchat.R
import gb.smartchat.SmartChatActivity
import gb.smartchat.databinding.FragmentCreateChatBinding
import gb.smartchat.entity.StoreInfo
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.ui.custom.MessageDialogFragment
import gb.smartchat.ui.custom.ProgressDialog
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable

class CreateChatFragment : Fragment() {

    companion object {
        private const val TAG = "CreateChatFragment"
        private const val PROGRESS_TAG = "progress tag"
        private const val ARG_STORE_INFO = "arg store info"

        fun create(storeInfo: StoreInfo) = CreateChatFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_STORE_INFO, storeInfo)
            }
        }
    }

    private val storeInfo by lazy {
        requireArguments().getSerializable(ARG_STORE_INFO) as StoreInfo
    }
    private var _binding: FragmentCreateChatBinding? = null
    private val binding: FragmentCreateChatBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: CreateChatViewModel by simpleViewModels {
        CreateChatViewModel(
            storeInfo,
            component.httpApi,
            CreateChatUDF.Store(),
            component.resourceManager
        )
    }

    private val contactsAdapter by lazy {
        ContactsAdapter(
            createGroupClickListener = {},
            contactClickListener = { viewModel.onContactClick(it) },
            errorActionClickListener = { viewModel.onErrorActionClick(it) }
        )
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
        registerOnBackPress {
            parentFragmentManager.popBackStack()
        }
        binding.appBarLayout.addSystemTopPadding()
        binding.toolbar.apply {
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            inflateMenu(R.menu.search)
            (menu.findItem(R.id.search).actionView as SearchView).setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.onQueryTextSubmit(query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.onQueryTextChange(newText)
                        return true
                    }
                }
            )
        }
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

        viewModel.navToChat
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    parentFragmentManager.replace(
                        ChatFragment.create(it),
                        NavAnim.SLIDE
                    )
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.showDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    MessageDialogFragment
                        .create(message = it)
                        .show(childFragmentManager, null)
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.progressDialog
            .subscribe { showProgressDialog(it) }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun showProgressDialog(progress: Boolean) {
        if (!isAdded) return

        val fragment = childFragmentManager.findFragmentByTag(PROGRESS_TAG)
        if (fragment != null && !progress) {
            (fragment as ProgressDialog).dismissAllowingStateLoss()
            childFragmentManager.executePendingTransactions()
        } else if (fragment == null && progress) {
            ProgressDialog().show(childFragmentManager, PROGRESS_TAG)
            childFragmentManager.executePendingTransactions()
        }
    }
}
