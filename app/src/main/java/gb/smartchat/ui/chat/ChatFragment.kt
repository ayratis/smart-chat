package gb.smartchat.ui.chat

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatBinding
import gb.smartchat.entity.Chat
import gb.smartchat.ui.SmartChatActivity
import gb.smartchat.ui.custom.ProgressDialog
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import kotlin.math.min


class ChatFragment : Fragment(), AttachDialogFragment.OnOptionSelected {

    companion object {
        private const val TAG = "ChatFragment"
        private const val PROGRESS_TAG = "progress_tag"
        private const val ARG_CHAT = "arg_chat"

        fun create(chat: Chat) = ChatFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_CHAT, chat)
            }
        }
    }

    private var _binding: FragmentChatBinding? = null
    private val binding: FragmentChatBinding get() = _binding!!
    private val argChat: Chat by lazy {
        requireArguments().getSerializable(ARG_CHAT) as Chat
    }
    private val compositeDisposable = CompositeDisposable()
    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }
    private val contentHelper by lazy {
        component.contentHelper
    }
    private val viewModel by viewModels<ChatViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ChatViewModel(
                    store = ChatUDF.Store(component.userId),
                    userId = component.userId,
                    chat = argChat,
                    socketApi = component.socketApi,
                    httpApi = component.httpApi,
                    contentHelper = contentHelper,
                    downloadHelper = component.fileDownloadHelper
                ) as T
            }
        }
    }
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
            },
            onQuoteListener = { chatItem ->
                viewModel.onQuoteMessage(chatItem.message)
            },
            nextPageUpCallback = {
                viewModel.loadNextPage(false)
            },
            nextPageDownCallback = {
                viewModel.loadNextPage(true)
            },
            onQuotedMsgClickListener = { chatItem ->
                viewModel.onQuotedMessageClick(chatItem)
            },
            onFileClickListener = { chatItem ->
                viewModel.onFileClick(chatItem)
            }
        )
    }
    private val requestCameraPermission =
        registerForActivityResult(RequestPermission()) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "require permission", Toast.LENGTH_SHORT).show()
            }
        }
    private var cameraPictureUri: Uri? = null
    private val getCameraImage = registerForActivityResult(TakePicture()) { isOK ->
        if (isOK) {
            cameraPictureUri?.let { viewModel.attach(it) }
        }
    }
    private val getPicFromGallery = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { viewModel.attach(it) }
        }
    }
    private val getContent = registerForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let { viewModel.attach(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback {
            parentFragmentManager.popBackStack()
        }
        binding.appBarLayout.addSystemTopPadding()
        binding.spacerBottom.addSystemBottomPadding()
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            itemAnimator = null
            adapter = chatAdapter
        }
        binding.etInput.doAfterTextChanged {
            viewModel.onTextChanged(it?.toString() ?: "")
        }
        binding.btnSend.setOnClickListener {
            viewModel.onSendClick()
        }
        binding.btnEditingClose.setOnClickListener {
            viewModel.onEditMessageReject()
        }
        binding.btnAttach.setOnClickListener {
            AttachDialogFragment().show(childFragmentManager, null)
        }
        binding.btnDetach.setOnClickListener {
            viewModel.detach()
        }
        binding.btnQuotedClose.setOnClickListener {
            viewModel.stopQuoting()
        }
        binding.rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var atBottom: Boolean = true
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val pos =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val isBottom = pos == chatAdapter.itemCount - 1
                if (atBottom != isBottom) {
                    atBottom = isBottom
                    viewModel.atBottomOfChat(isBottom)
                }
            }
        })
        binding.tvUnreadMessageCount.setOnClickListener {
            viewModel.scrollToBottom()
        }
        binding.btnEmptyRetry.setOnClickListener {
            viewModel.emptyRetry()
        }
    }

    private var scrollToPosition: SingleEvent<Int>? = null
    private val defaultOffset: Int by lazy {
        resources.displayMetrics.heightPixels / 3
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable.addAll(
            viewModel.viewState
                .map { it.chatItems }
                .distinctUntilChanged()
                .subscribe { chatItems ->
                    Log.d(TAG, "chatItemsSize: ${chatItems.size}")
                    Log.d(TAG, "chatItem10: ${chatItems.getOrNull(19)}")
                    Log.d(
                        TAG,
                        "need to scroll to position: ${scrollToPosition?.getContentIfNotHandled()}"
                    )
                    chatAdapter.submitList(chatItems) {
                        scrollToPosition?.getContentIfNotHandled()?.let { pos ->
                            Log.d(TAG, "scrolling to position: $pos")
                            val targetPosition = min(pos, chatItems.lastIndex)
                            if (targetPosition != chatItems.lastIndex) {
                                (binding.rvChat.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                    targetPosition,
                                    defaultOffset
                                )
                            } else {
                                binding.rvChat.scrollToPosition(targetPosition)
                            }
                        }
                    }
                },
            viewModel.viewState
                .map { listOf(it.editingMessage) }
                .distinctUntilChanged()
                .subscribe {
                    val editingMessage = it.first()
                    binding.btnAttach.visible(editingMessage == null)
                    binding.viewEditingMessage.visible(editingMessage != null)
                    binding.tvEditingMessage.text = editingMessage?.text
                    binding.btnSend.setImageResource(
                        if (editingMessage != null) R.drawable.ic_baseline_check_24
                        else R.drawable.btn_send
                    )
                },
            viewModel.viewState
                .map { it.typingSenderIds }
                .distinctUntilChanged()
                .subscribe { typingSenderIds ->
//                    binding.toolbar.subtitle =
//                        if (typingSenderIds.isEmpty()) ""
//                        else "typing: $typingSenderIds"
                },
            viewModel.viewState
                .map { it.attachmentState }
                .distinctUntilChanged()
                .subscribe { attachmentState ->
                    Log.d(TAG, "attachmentState: $attachmentState")
                    fun showContent(uri: Uri, isProgress: Boolean) {
                        val mimeType = contentHelper.mimeType(uri)
                        val isImage = mimeType?.startsWith("image") == true
                        if (isImage) {
                            binding.viewFileAttachment.visible(false)
                            binding.progressBarPhoto.visible(isProgress)
                            binding.ivAttachmentPhoto.visible(true)
                            Glide.with(binding.ivAttachmentPhoto)
                                .load(uri)
                                .transform(
                                    CenterCrop(),
                                    RoundedCorners(12.dp(binding.ivAttachmentPhoto))
                                )
                                .into(binding.ivAttachmentPhoto)
                        } else {
                            binding.viewFileAttachment.visible(true)
                            binding.progressBarPhoto.visible(false)
                            binding.ivAttachmentPhoto.visible(false)
                            val nameSize = contentHelper.nameSize(uri)
                            binding.ivFile.visible(!isProgress)
                            binding.progressBarFile.visible(isProgress)
                            binding.tvFileName.text = nameSize?.first
                            binding.tvFileSize.text =
                                nameSize?.second?.let { "${it / 1000} Kb" }
                        }
                    }
                    when (attachmentState) {
                        is ChatUDF.AttachmentState.Empty -> {
                            binding.viewAttachment.visible(false)
                        }
                        is ChatUDF.AttachmentState.Uploading -> {
                            binding.viewAttachment.visible(true)
                            showContent(attachmentState.uri, true)
                        }
                        is ChatUDF.AttachmentState.UploadSuccess -> {
                            binding.viewAttachment.visible(true)
                            showContent(attachmentState.uri, false)
                        }
                    }
                },
            viewModel.viewState
                .map { listOf(it.quotingMessage) }
                .distinctUntilChanged()
                .subscribe {
                    val quotingMessage = it.first()
                    binding.viewQuotedMessage.visible(quotingMessage != null)
                    binding.tvQuotedMessage.text = quotingMessage?.text
                },
            viewModel.viewState
                .map { it.pagingState }
                .distinctUntilChanged()
                .subscribe { pagingState ->
                    when (pagingState) {
                        ChatUDF.PagingState.EMPTY_ERROR -> {
                            binding.viewEmptyError.visible(true)
                            showProgressDialog(false)
                        }
                        ChatUDF.PagingState.EMPTY_PROGRESS -> {
                            binding.viewEmptyError.visible(false)
                            showProgressDialog(true)
                        }
                        else -> {
                            showProgressDialog(false)
                            binding.viewEmptyError.visible(false)
                        }
                    }
                },
            viewModel.viewState
                .map { it.fullDataUp to it.fullDataDown }
                .distinctUntilChanged()
                .subscribe { (fullDataUp, fullDataDown) ->
                    chatAdapter.fullDataUp = fullDataUp
                    chatAdapter.fullDataDown = fullDataDown
                },
            viewModel.viewState
                .map { it.isOnline }
                .distinctUntilChanged()
                .subscribe { isOnline ->
                    binding.toolbar.title = if (isOnline) "Снабжение" else "Online: $isOnline"
                },
            viewModel.viewState
                .map { listOf(it.withScrollTo) }
                .distinctUntilChanged()
                .subscribe { event ->
                    scrollToPosition = event.first()
                },
            viewModel.viewState
                .map { it.unreadMessageCount }
                .distinctUntilChanged()
                .subscribe { unreadMessageCount ->
                    val text =
                        if (unreadMessageCount == ChatUDF.UNREAD_OVER_MAX_COUNT) {
                            "${ChatUDF.DEFAULT_PAGE_SIZE}+"
                        } else {
                            unreadMessageCount.toString()
                        }
                    binding.tvUnreadMessageCount.visible(unreadMessageCount != 0)
                    binding.tvUnreadMessageCount.text = text
                },
            viewModel.viewState
                .map { it.sendEnabled }
                .distinctUntilChanged()
                .subscribe { enabled ->
                    binding.btnSend.isEnabled = enabled
                },


            viewModel.instantScrollTo
                .subscribe {
                    (binding.rvChat.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        it,
                        defaultOffset
                    )
                },

            viewModel.setInputText
                .subscribe { singleEvent ->
                    singleEvent.getContentIfNotHandled()?.let {
                        binding.etInput.setText(it)
                        binding.etInput.setSelection(it.length)
                    }
                },
            viewModel.openFile
                .subscribe { singleEvent ->
                    singleEvent.getContentIfNotHandled()?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = uri
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.file_open_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        )
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onSelectFromGallery() {
        getPicFromGallery.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        )
    }

    override fun onAttachFile() {
        getContent.launch("*/*")
    }

    override fun onTakePhoto() {
        if (isCameraPermissionGranted()) takePhoto()
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun takePhoto() {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "JPEG_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
        val cameraPictureUri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        this.cameraPictureUri = cameraPictureUri
        getCameraImage.launch(cameraPictureUri)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
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
