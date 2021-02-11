package gb.smartchat.ui.chat

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class TakePictureDialogFragment : DialogFragment() {

    private val clickListener
        get() = when {
            parentFragment is OnOptionSelected -> parentFragment as OnOptionSelected
            activity is OnOptionSelected -> activity as OnOptionSelected
            else -> object : OnOptionSelected {}
        }

    private val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext()).apply {
            setItems(options) { _, which ->
                if (options[which] == "Take Photo") {
                    clickListener.onTakePhoto()
                } else {
                    clickListener.onSelectFromGallery()
                }
            }
        }.create()


    interface OnOptionSelected {
        fun onTakePhoto() {}
        fun onSelectFromGallery() {}
    }
}