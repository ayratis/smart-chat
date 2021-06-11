package gb.smartchat.library.utils

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.ColorInt

class AppClickableSpan(
    private val isUnderlineText: Boolean,
    @ColorInt private val linkColor: Int,
    private val clickListener: () -> Unit
): ClickableSpan() {

    override fun updateDrawState(ds: TextPaint) {
        ds.color = linkColor
        ds.bgColor = 0
        ds.isUnderlineText = isUnderlineText
    }

    override fun onClick(widget: View) {
        clickListener.invoke()
    }
}
