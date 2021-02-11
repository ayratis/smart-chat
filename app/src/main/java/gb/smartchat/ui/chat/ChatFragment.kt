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
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.FragmentChatBinding
import gb.smartchat.di.InstanceFactory
import gb.smartchat.ui.chat.state_machine.Store
import gb.smartchat.utils.addSystemBottomPadding
import gb.smartchat.utils.addSystemTopPadding
import gb.smartchat.utils.visible
import io.reactivex.disposables.CompositeDisposable
import java.io.File


class ChatFragment : Fragment(R.layout.fragment_chat), TakePictureDialogFragment.OnOptionSelected {

    companion object {
        private const val TAG = "ChatFragment"
        private const val ARG_USER_ID = "arg_user_id"
        private const val ARG_CHAT_ID = "arg_chat_id"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUEST_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PHOTO = 1
        private const val REQUEST_CODE_GALLERY = 2
        private const val REQUEST_CODE_FILE = 3

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
        binding.btnAttachFile.setOnClickListener {
            takeFile()
        }
        binding.btnAttachFileClose.setOnClickListener {
            viewModel.detachFile()
        }
        binding.btnAttachPhoto.setOnClickListener {
            TakePictureDialogFragment().show(childFragmentManager, null)
        }
        binding.btnAttachPhotoClose.setOnClickListener {
            viewModel.detachPhoto()
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
                    val pos = (binding.rvChat.layoutManager as LinearLayoutManager)
                        .findLastVisibleItemPosition()
                    chatAdapter.submitList(chatItems) {
                        if (chatItems.lastOrNull() is ChatItem.Outgoing ||
                            pos == chatItems.lastIndex - 1
                        ) {
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
                    binding.btnMainAction.setImageResource(
                        if (editingMessage != null) R.drawable.ic_baseline_check_24
                        else R.drawable.ic_baseline_send_24
                    )
                },
            viewModel.viewState
                .map { it.typingSenderIds }
                .distinctUntilChanged()
                .subscribe { typingSenderIds ->
                    binding.toolbar.subtitle =
                        if (typingSenderIds.isEmpty()) ""
                        else "typing: $typingSenderIds"
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
                                    val fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                                    binding.tvAttachFileName.text = fileName
                                }
                            }
                    }
                },
            viewModel.setInputText
                .subscribe { singleEvent ->
                    singleEvent.getContentIfNotHandled()?.let {
                        binding.etInput.setText(it)
                        binding.etInput.setSelection(it.length)
                    }
                }
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
}