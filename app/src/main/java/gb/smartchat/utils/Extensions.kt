package gb.smartchat.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import gb.smartchat.R
import io.reactivex.disposables.Disposable
import kotlin.math.roundToInt


fun Int.dp(displayMetrics: DisplayMetrics): Int =
    (this.toFloat() * (displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()

fun Int.dp(res: Resources): Int = this.dp(res.displayMetrics)
fun Int.dp(context: Context): Int = this.dp(context.resources.displayMetrics)
fun Int.dp(view: View): Int = this.dp(view.resources.displayMetrics)

@ColorInt
fun Int.adjustAlpha(factor: Float): Int {
    val alpha = (Color.alpha(this) * factor).roundToInt()
    val red: Int = Color.red(this)
    val green: Int = Color.green(this)
    val blue: Int = Color.blue(this)
    return Color.argb(alpha, red, green, blue)
}

fun View.updatePadding(
    left: Int = paddingLeft,
    top: Int = paddingTop,
    right: Int = paddingRight,
    bottom: Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}

fun View.addSystemTopPadding(
    targetView: View = this,
    isConsumed: Boolean = false
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        doOnApplyWindowInsets { _, insets, initialPadding ->
            targetView.updatePadding(
                top = initialPadding.top + insets.systemWindowInsetTop
            )
            if (isConsumed) {
                WindowInsetsCompat.Builder(insets).setSystemWindowInsets(
                    Insets.of(
                        insets.systemWindowInsetLeft,
                        0,
                        insets.systemWindowInsetRight,
                        insets.systemWindowInsetBottom
                    )
                ).build()
            } else {
                insets
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
fun View.addSystemTopPadding(
    targetViews: List<View>,
    isConsumed: Boolean = false
) {
    doOnApplyWindowInsets { _, insets, initialPadding ->
        targetViews.forEach {
            it.updatePadding(
                top = initialPadding.top + insets.systemWindowInsetTop
            )
        }
        if (isConsumed) {
            WindowInsetsCompat.Builder(insets).setSystemWindowInsets(
                Insets.of(
                    insets.systemWindowInsetLeft,
                    0,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            ).build()
        } else {
            insets
        }
    }
}

fun View.addSystemBottomPadding(
    targetView: View = this,
    isConsumed: Boolean = false
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        doOnApplyWindowInsets { _, insets, initialPadding ->
            targetView.updatePadding(
                bottom = initialPadding.bottom + insets.systemWindowInsetBottom
            )
            if (isConsumed) {
                WindowInsetsCompat.Builder(insets).setSystemWindowInsets(
                    Insets.of(
                        insets.systemWindowInsetLeft,
                        insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight,
                        0
                    )
                ).build()
            } else {
                insets
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
fun View.doOnApplyWindowInsets(block: (View, insets: WindowInsetsCompat, initialPadding: Rect) -> WindowInsetsCompat) {
    val initialPadding = recordInitialPaddingForView(this)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        block(v, insets, initialPadding)
    }
    requestApplyInsetsWhenAttached()
}

private fun recordInitialPaddingForView(view: View) =
    Rect(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)

@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(v)
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Window.configureSystemBars() {
    decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        decorView.systemUiVisibility = decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        statusBarColor = ContextCompat.getColor(context, android.R.color.transparent)
    } else {
        statusBarColor = ContextCompat.getColor(context, R.color.status_bar_color)
    }


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        decorView.systemUiVisibility = decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    navigationBarColor = ContextCompat.getColor(context, R.color.navigation_bar_color)
}

fun View.visible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)

fun Context.colorFromAttr(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

fun Context.color(colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun Disposable.disposeOnPause(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            this@disposeOnPause.dispose()
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}
