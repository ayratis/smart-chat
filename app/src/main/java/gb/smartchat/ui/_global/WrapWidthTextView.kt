package gb.smartchat.ui._global

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.ceil


class WrapWidthTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val usrMaxWidth = resources.displayMetrics.widthPixels * 0.7f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val layout = layout
        if (layout != null) {
            val width = (ceil(getMaxLineWidth(layout).toDouble()).toInt()
                    + compoundPaddingLeft + compoundPaddingRight)
            val height = measuredHeight
            setMeasuredDimension(width, height)
        }
    }

    private fun getMaxLineWidth(layout: Layout): Float {
        var maxWidth = 0.0f
        val lines = layout.lineCount
        for (i in 0 until lines) {
            if (layout.getLineWidth(i) > maxWidth) {
                maxWidth = layout.getLineWidth(i)
            }
        }

//        return maxWidth
        return if (maxWidth <= usrMaxWidth) maxWidth else usrMaxWidth
    }
}
