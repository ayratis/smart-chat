package gb.smartchat.library.ui.service_chat_preparing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import gb.smartchat.R
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui.chat.ChatFragment
import gb.smartchat.library.utils.registerOnBackPress
import gb.smartchat.library.utils.replace
import io.reactivex.disposables.CompositeDisposable

class ServiceChatPreparingFragment : Fragment(), MessageDialogFragment.OnClickListener {

    companion object {
        private const val ERROR_DIALOG_TAG = "error dialog tag"

        fun create() = ServiceChatPreparingFragment()
    }

    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }

    private val viewModel: ServiceChatPreparingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(p0: Class<T>): T {
                return ServiceChatPreparingViewModel(
                    component.httpApi,
                    component.resourceManager,
                    component.storeInfoList.first()
                ) as T
            }
        }
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerOnBackPress {
            activity?.finish()
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.navToServiceChat
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { chat ->
                    parentFragmentManager.replace(ChatFragment.create(chat.id, chat))
                }
            }
            .also { compositeDisposable.add(it) }

        viewModel.showDialog
            .subscribe { event ->
                event.getContentIfNotHandled()?.let { message ->
                    MessageDialogFragment
                        .create(message = message, tag = ERROR_DIALOG_TAG)
                        .show(childFragmentManager, ERROR_DIALOG_TAG)
                }
            }
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun dialogCanceled(tag: String) {
        activity?.finish()
    }

    override fun dialogPositiveClicked(tag: String) {
        activity?.finish()
    }
}