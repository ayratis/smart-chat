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
import androidx.core.view.updatePadding
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatBinding
import gb.smartchat.entity.Chat
import gb.smartchat.ui.SmartChatActivity
import gb.smartchat.ui.custom.CenterSmoothScroller
import gb.smartchat.ui.custom.HeaderItemDecoration
import gb.smartchat.ui.custom.ProgressDialog
import gb.smartchat.utils.*
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import kotlin.math.max
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
    private val renderDisposables = CompositeDisposable()
    private val newsDisposables = CompositeDisposable()
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
                    store = ChatUDF.Store(
                        component.userId,
                        argChat.getReadInfo(component.userId),
                        argChat.users
                    ),
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
    private val linearLayoutManager by lazy {
        LinearLayoutManager(binding.rvChat.context).apply {
            stackFromEnd = true
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
                Log.d(
                    TAG,
                    "nextPageUpCallback: isSmoothScrolling: ${linearLayoutManager.isSmoothScrolling}"
                )
                if (!linearLayoutManager.isSmoothScrolling) {
                    viewModel.loadNextPage(false)
                }
            },
            nextPageDownCallback = {
                Log.d(
                    TAG,
                    "nextPageDownCallback: isSmoothScrolling: ${linearLayoutManager.isSmoothScrolling}"
                )
                if (!linearLayoutManager.isSmoothScrolling) {
                    viewModel.loadNextPage(true)
                }
            },
            onQuotedMsgClickListener = { chatItem ->
                viewModel.onQuotedMessageClick(chatItem)
            },
            onFileClickListener = { chatItem ->
                viewModel.onFileClick(chatItem)
            },
            onMentionClickListener = { mention ->
                Toast
                    .makeText(requireContext(), "user_id: ${mention.userId}", Toast.LENGTH_SHORT)
                    .show()
            }
        ).apply {
            setHasStableIds(true)
        }
    }
    private val mentionAdapter by lazy {
        MentionAdapter { user ->
            viewModel.onMentionClick(user)
        }
    }
    private val mentionSheetBehavior: BottomSheetBehavior<RecyclerView> by lazy {
        BottomSheetBehavior.from(binding.rvMentions)
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
    private val onLayoutChangeListener =
        View.OnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val height = bottom - top
            binding.rvChat.updatePadding(bottom = height)
            binding.rvMentions.updatePadding(bottom = height)
            mentionSheetBehavior.peekHeight = height + 126.dp(binding.root)
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
        renderDisposables.clear()
        binding.layoutInput.removeOnLayoutChangeListener(onLayoutChangeListener)
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback {
            parentFragmentManager.popBackStack()
        }
        binding.layoutInput.addOnLayoutChangeListener(onLayoutChangeListener)
        binding.appBarLayout.addSystemTopPadding()
        binding.layoutInput.addSystemBottomPadding()
        binding.toolbar.apply {
            title = argChat.storeName
            subtitle = argChat.agentName
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        binding.rvChat.apply {
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            itemAnimator = null
            isNestedScrollingEnabled = false
            adapter = chatAdapter
            addItemDecoration(HeaderItemDecoration(this, false))
        }
        binding.rvMentions.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            adapter = mentionAdapter
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
                val pos = linearLayoutManager.findLastVisibleItemPosition()
                val isBottom = pos == chatAdapter.itemCount - 1
                if (atBottom != isBottom) {
                    atBottom = isBottom
                    viewModel.atBottomOfChat(isBottom)
                }
            }
        })
        binding.btnScrollDown.setOnClickListener {
            viewModel.scrollToBottom()
        }
        binding.btnEmptyRetry.setOnClickListener {
            viewModel.emptyRetry()
        }
        renderViewModel()
    }

    override fun onResume() {
        super.onResume()
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
            .also { newsDisposables.add(it) }
    }

    override fun onPause() {
        newsDisposables.clear()
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

    private fun renderViewModel() {
        viewModel.chatItems
            .subscribe { (chatItems, scrollOptions) ->
                Log.d(TAG, "chatItemsSize: ${chatItems.size}, scrollOptions: $scrollOptions")
                chatAdapter.submitList(chatItems) {
                    scrollOptions?.let { (position, fake, isUp) ->
                        if (fake) {
                            if (position == chatItems.lastIndex) {
                                fakeScrollToBottom()
                            } else {
                                fakeScrollToCenterPosition(position, isUp)
                            }
                        } else {
                            instantScrollToPosition(position)
                        }
                    }
                }
            }
            .also { renderDisposables.add(it) }

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
            }
            .also { renderDisposables.add(it) }
        viewModel.viewState
            .map { it.mentions }
            .subscribe { mentionAdapter.setData(it) }
            .also { renderDisposables.add(it) }
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
            }
            .also { renderDisposables.add(it) }
        viewModel.viewState
            .map { listOf(it.quotingMessage) }
            .distinctUntilChanged()
            .subscribe {
                val quotingMessage = it.first()
                binding.viewQuotedMessage.visible(quotingMessage != null)
                binding.tvQuotedMessage.text = quotingMessage?.text
            }
            .also { renderDisposables.add(it) }
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
                        showProgressDialog(false)
                    }
                    else -> {
                        showProgressDialog(false)
                        binding.viewEmptyError.visible(false)
                    }
                }
            }
            .also { renderDisposables.add(it) }
        viewModel.viewState
            .map { it.fullDataUp to it.fullDataDown }
            .distinctUntilChanged()
            .subscribe { (fullDataUp, fullDataDown) ->
                chatAdapter.fullDataUp = fullDataUp
                chatAdapter.fullDataDown = fullDataDown
            }
            .also { renderDisposables.add(it) }
        viewModel.viewState
            .map { it.isOnline }
            .distinctUntilChanged()
            .subscribe { isOnline ->
                binding.toolbar.apply {
                    if (isOnline) {
                        setSubtitleTextColor(context.color(R.color.razzmatazz))
                        subtitle = argChat.agentName
                    } else {
                        setSubtitleTextColor(context.color(R.color.santas_gray))
                        subtitle = getString(R.string.waiting_for_network)
                    }
                }
            }
            .also { renderDisposables.add(it) }
        viewModel.viewState
            .map { it.readInfo.unreadCount }
            .distinctUntilChanged()
            .subscribe { unreadMessageCount ->
                binding.tvUnreadMessageCount.visible(unreadMessageCount > 0)
                binding.tvUnreadMessageCount.text = unreadMessageCount.toString()
            }
            .also { renderDisposables.add(it) }
        viewModel.viewState
            .map { it.sendEnabled }
            .distinctUntilChanged()
            .subscribe { enabled ->
                binding.btnSend.isEnabled = enabled
            }
            .also { renderDisposables.add(it) }
        viewModel.setInputText
            .subscribe { singleEvent ->
                singleEvent.getContentIfNotHandled()?.let {
                    binding.etInput.setText(it)
                    binding.etInput.setSelection(it.length)
                }
            }
            .also { renderDisposables.add(it) }
        viewModel.viewState
            .map { it.atBottom to it.fullDataDown }
            .distinctUntilChanged()
            .subscribe { (atBottom, fullDataDown) ->
                binding.btnScrollDown.visible(!atBottom || !fullDataDown)
            }
            .also { renderDisposables.add(it) }
    }

    private fun fakeScrollToCenterPosition(pos: Int, isUpScroll: Boolean) {
        val beforePos = if (isUpScroll) chatAdapter.itemCount - 1 else 0
        Log.d(
            TAG,
            "fakeScrollToPosition: pos: $pos, isUpScroll: $isUpScroll, beforePos: $beforePos"
        )
        linearLayoutManager.scrollToPosition(beforePos)
        val scroller = CenterSmoothScroller(requireContext()).apply {
            targetPosition = pos
        }
        linearLayoutManager.startSmoothScroll(scroller)
    }

    private fun fakeScrollToBottom() {
        if (chatAdapter.itemCount - 10 > 0) {
            val beforePos = chatAdapter.itemCount - 10
            Log.d(TAG, "fakeScrollToBottom: beforePos: $beforePos")
            binding.rvChat.scrollToPosition(beforePos)
        }
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)

    }

    private fun instantScrollToPosition(pos: Int) {
        val scroller = CenterSmoothScroller(requireContext()).apply {
            targetPosition = pos
        }
        val firstVisiblePos = linearLayoutManager.findFirstVisibleItemPosition()
        val lastVisiblePos = linearLayoutManager.findLastVisibleItemPosition()

        if (pos - 1 <= lastVisiblePos) {
            //если находимся внизу
            binding.rvChat.scrollToPosition(pos)
            return
        }

        val firstClosePos = max(firstVisiblePos - 10, 0)
        val lastClosePos = min(lastVisiblePos + 10, chatAdapter.itemCount - 1)
        Log.d(
            TAG, "instantScrollToPosition: pos: $pos, " +
                    "firstVisiblePos: $firstVisiblePos, " +
                    "lastVisiblePos: $lastVisiblePos, " +
                    "firstClosePos: $firstClosePos, " +
                    "lastClosePos: $lastClosePos"
        )
        if (pos in firstClosePos..lastClosePos) {
            linearLayoutManager.startSmoothScroll(scroller)
            return
        }
        val beforePos =
            if (pos < firstClosePos) pos + 10
            else pos - 10
        Log.d(TAG, "instantScrollToPosition: beforePos: $beforePos")
        linearLayoutManager.scrollToPosition(beforePos)
        linearLayoutManager.startSmoothScroll(scroller)
    }
}
