package gb.smartchat.library.ui.image_viewer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import gb.smartchat.R

class ImageViewerDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_URL = "arg url"

        fun create(url: String): ImageViewerDialogFragment =
            ImageViewerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }
    }

    private val imageUrl: String by lazy {
        requireArguments().getString(ARG_URL)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return LayoutInflater.from(context).inflate(R.layout.fragment_image_viewer, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            dismiss()
        }
        view.findViewById<ImageView>(R.id.btn_share).setOnClickListener {
            Glide.with(this)
                .asFile()
                .load(imageUrl)
                .into(object : CustomTarget<java.io.File>() {
                    override fun onResourceReady(
                        resource: java.io.File,
                        transition: Transition<in java.io.File>?
                    ) {
                        val fileUri: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            "gb.smartchat.library.imagefileprovider",
                            resource
                        )
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            type = "image/*"
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }
                })
        }
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    view.findViewById<SubsamplingScaleImageView>(R.id.iv_image)
                        .setImage(ImageSource.bitmap(resource))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }

            })
    }
}
