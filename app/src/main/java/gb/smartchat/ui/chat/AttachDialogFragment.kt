package gb.smartchat.ui.chat

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class AttachDialogFragment : DialogFragment() {

    private val clickListener
        get() = when {
            parentFragment is OnOptionSelected -> parentFragment as OnOptionSelected
            activity is OnOptionSelected -> activity as OnOptionSelected
            else -> object : OnOptionSelected {}
        }

    private val options = arrayOf<CharSequence>("Take Photo", "Gallery", "File")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext()).apply {
            setItems(options) { _, which ->
                when (which) {
                    0 -> clickListener.onTakePhoto()
                    1 -> clickListener.onSelectFromGallery()
                    else -> clickListener.onAttachFile()
                }
            }
        }.create()


    interface OnOptionSelected {
        fun onTakePhoto() {}
        fun onSelectFromGallery() {}
        fun onAttachFile() {}
    }
}