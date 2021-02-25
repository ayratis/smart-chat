package gb.smartchat.ui.chat

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatBinding
import gb.smartchat.ui.SmartChatActivity
import gb.smartchat.ui.chat.state_machine.PagingState
import gb.smartchat.ui.chat.state_machine.State
import gb.smartchat.ui.custom.ProgressDialog
import gb.smartchat.utils.SingleEvent
import gb.smartchat.utils.addSystemBottomPadding
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.visible
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import kotlin.math.min


class ChatFragment : Fragment(R.layout.fragment_chat), AttachDialogFragment.OnOptionSelected {

    companion object {
        private const val TAG = "ChatFragment"
        private const val PROGRESS_TAG = "progress_tag"
        private const val ARG_CHAT_ID = "arg_chat_id"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUEST_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PHOTO = 1
        private const val REQUEST_CODE_GALLERY = 2
        private const val REQUEST_CODE_FILE = 3


        fun create(chatId: Long) = ChatFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_CHAT_ID, chatId)
            }
        }
    }

    private val argChatId: Long by lazy {
        requireArguments().getLong(ARG_CHAT_ID)
    }
    private val component by lazy {
        (requireActivity() as SmartChatActivity).component
    }
    private val viewModel by viewModels<ChatViewModel> {
        ChatViewModel.Factory(
            userId = component.userId,
            chatId = argChatId,
            socketApi = component.socketApi,
            httpApi = component.httpApi
        )
    }
    private val binding by viewBinding(FragmentChatBinding::bind)
    private val compositeDisposable = CompositeDisposable()
    private var takePhotoUri: Uri? = null

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
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBarLayout.addSystemTopPadding()
        binding.spacerBottom.addSystemBottomPadding()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            itemAnimator = null
            adapter = chatAdapter
        }
        binding.etInput.doAfterTextChanged {
            viewModel.onTextChanged(it?.toString() ?: "")
            val tintColor = ContextCompat.getColor(
                requireContext(),
                if (it?.toString()?.isBlank() == false) R.color.purple_heart else R.color.gray
            )
            DrawableCompat.setTint(binding.btnMainAction.drawable, tintColor)
        }
        binding.btnMainAction.setOnClickListener {
            viewModel.onSendClick()
        }
        binding.btnEditingClose.setOnClickListener {
            viewModel.onEditMessageReject()
        }
        binding.btnAttach.setOnClickListener {
            AttachDialogFragment().show(childFragmentManager, null)
        }
        binding.btnAttachFileClose.setOnClickListener {
            viewModel.detachFile()
        }
        binding.btnAttachPhotoClose.setOnClickListener {
            viewModel.detachPhoto()
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

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
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
                    binding.viewEditingMessage.visible(editingMessage != null)
                    binding.tvEditingMessage.text = editingMessage?.text
                    binding.btnMainAction.setImageResource(
                        if (editingMessage != null) R.drawable.ic_baseline_check_24
                        else R.drawable.ic_send_24
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
                .map { listOf(it.attachedPhoto) }
                .distinctUntilChanged()
                .subscribe {
                    val uri = it.first()
                    binding.viewAttachPhoto.visible(uri != null)
                    Glide.with(binding.ivPhoto)
                        .load(uri)
                        .centerCrop()
                        .into(binding.ivPhoto)
                },
            viewModel.viewState
                .map { listOf(it.attachedFile) }
                .distinctUntilChanged()
                .subscribe {
                    val uri = it.first()
                    binding.viewAttachFile.visible(uri != null)
                    binding.tvAttachFileName.text = null
                    if (uri?.scheme == "content") {
                        requireContext().contentResolver
                            .query(uri, null, null, null, null)
                            ?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val fileName =
                                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                                    binding.tvAttachFileName.text = fileName
                                }
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
                        PagingState.EMPTY_ERROR -> {
                            binding.viewEmptyError.visible(true)
                            showProgressDialog(false)
                        }
                        PagingState.EMPTY_PROGRESS -> {
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
                        if (unreadMessageCount == State.UNREAD_OVER_MAX_COUNT) {
                            "${State.DEFAULT_PAGE_SIZE}+"
                        } else {
                            unreadMessageCount.toString()
                        }
                    binding.tvUnreadMessageCount.visible(unreadMessageCount != 0)
                    binding.tvUnreadMessageCount.text = text
                },
            viewModel.instaScrollTo
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
        )
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "require permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onTakePhoto() {
        if (allPermissionsGranted()) takePhoto()
        else requestPermissions(REQUEST_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    override fun onSelectFromGallery() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhoto, REQUEST_CODE_GALLERY)
    }

    override fun onAttachFile() {
        takeFile()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PHOTO -> {
                    takePhotoUri?.let { viewModel.attachPhoto(it) }
                }
                REQUEST_CODE_GALLERY -> {
                    data?.data?.let { viewModel.attachPhoto(it) }
                }
                REQUEST_CODE_FILE -> {
                    data?.data?.let { viewModel.attachFile(it) }
                }
            }
        }
    }

    private fun takePhoto() {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "JPEG_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
        context?.run {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePicIntent ->
                takePicIntent.resolveActivity(packageManager)?.also {

                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "$packageName.fileprovider",
                        file
                    )
                    takePhotoUri = photoURI
                    takePicIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePicIntent, REQUEST_CODE_PHOTO)
                }
            }
        }
    }

    private fun takeFile() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, REQUEST_CODE_FILE)
    }

    private fun allPermissionsGranted(): Boolean {
        context?.run {
            for (permission in REQUEST_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED
                ) return false
            }
            return true
        }
        return false
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