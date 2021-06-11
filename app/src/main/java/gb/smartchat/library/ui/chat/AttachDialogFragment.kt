package gb.smartchat.library.ui.chat

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import gb.smartchat.R
import java.io.File

class AttachDialogFragment : DialogFragment() {

    interface Listener {
        fun onCameraPicture(uri: Uri) {}
        fun onPhotoFromGallery(uri: Uri) {}
        fun onAttachFile(uri: Uri) {}
    }

    companion object {
        private const val TAG = "AttachDialogFragment"
        private const val ARG_CAMERA = "arg camera"
        private const val ARG_GALLERY = "arg gallery"
        private const val ARG_FILES = "arg files"

        fun create(
            camera: Boolean,
            gallery: Boolean,
            files: Boolean
        ) = AttachDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_CAMERA, camera)
                putBoolean(ARG_GALLERY, gallery)
                putBoolean(ARG_FILES, files)
            }
        }
    }

    private val isCamera: Boolean by lazy {
        requireArguments().getBoolean(ARG_CAMERA)
    }

    private val isGallery: Boolean by lazy {
        requireArguments().getBoolean(ARG_GALLERY)
    }

    private val isFiles: Boolean by lazy {
        requireArguments().getBoolean(ARG_FILES)
    }

    private val listener
        get() = when {
            parentFragment is Listener -> parentFragment as Listener
            activity is Listener -> activity as Listener
            else -> object : Listener {}
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "require permission", Toast.LENGTH_SHORT).show()
            }
        }
    private var cameraPictureUri: Uri? = null
    private val getCameraImage =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isOK ->
            if (isOK) {
                cameraPictureUri?.let(listener::onCameraPicture)
            }
            dismiss()
        }
    private val getPicFromGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let(listener::onPhotoFromGallery)
            }
            dismiss()
        }
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            Log.d(TAG, "getContentResult: $uri")
            uri?.let(listener::onAttachFile)
            dismiss()
        }

    private val cameraOptionName by lazy {
        getString(R.string.camera)
    }

    private val galleryOptionName by lazy {
        getString(R.string.gallery)
    }

    private val filesOptionName by lazy {
        getString(R.string.files)
    }

    private val options by lazy {
        val list = mutableListOf<String>()
        if (isCamera) list += cameraOptionName
        if (isGallery) list += galleryOptionName
        if (isFiles) list += filesOptionName
        list.toTypedArray()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setItems(options, null)
            .create()
            .apply {
                listView.setOnItemClickListener { _, _, position, _ ->
                    when (options[position]) {
                        cameraOptionName -> {
                            if (isCameraPermissionGranted()) takePhoto()
                            else requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                        galleryOptionName -> {
                            getPicFromGallery.launch(
                                Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                            )
                        }
                        filesOptionName -> {
                            getContent.launch("*/*")
                        }
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
}
