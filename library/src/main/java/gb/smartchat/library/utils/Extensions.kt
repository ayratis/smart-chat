package gb.smartchat.library.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import gb.smartchat.R
import gb.smartchat.library.data.download.DownloadStatus
import gb.smartchat.library.data.download.FileDownloadHelper
import gb.smartchat.library.data.resources.ResourceManager
import gb.smartchat.library.entity.*
import gb.smartchat.library.entity.request.CreateChatRequest
import gb.smartchat.library.entity.request.MessageCreateRequest
import gb.smartchat.library.entity.request.MessageDeleteRequest
import gb.smartchat.library.entity.request.MessageEditRequest
import io.reactivex.disposables.Disposable
import java.io.IOException
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
    val isTablet = context.resources.getBoolean(R.bool.tablet)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !isTablet) {
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

fun View.addSystemBottomPadding(
    targetView: View = this,
    isConsumed: Boolean = false
) {
    val isTablet = context.resources.getBoolean(R.bool.tablet)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !isTablet) {
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

fun Context.drawable(@DrawableRes drawableRes: Int): Drawable =
    ContextCompat.getDrawable(this, drawableRes)!!

fun Disposable.disposeOnPause(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            this@disposeOnPause.dispose()
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

fun Intent.toLogsString(): String {
    val stringBuilder = StringBuilder()
    val bundle = extras
    if (bundle != null) {
        var first = true
        for (key in bundle.keySet()) {
            if (first) {
                first = false
            } else {
                stringBuilder.append("\n")
            }
            stringBuilder.append(key + " : " + if (bundle.get(key) != null) bundle.get(key) else "NULL")
        }
    }
    return stringBuilder.toString()
}

fun Message.composeWithDownloadStatus(downloadHelper: FileDownloadHelper): Message {
    file?.let { file ->
        file.url?.let { url ->
            val downloadStatus = downloadHelper.getDownloadStatus(url)
            return this.copy(file = file.copy(downloadStatus = downloadStatus))
        }
        return this.copy(file = file.copy(downloadStatus = DownloadStatus.Empty))
    }
    return this
}

fun Message.composeWithUser(users: List<User>): Message {
    val user = users.find { it.id == this.senderId }
    return this.copy(user = user)
}

fun Message.toMessageCreateRequestBody(): MessageCreateRequest? {
    return if (text != null && senderId != null && chatId != null && clientId != null) {
        MessageCreateRequest(
            text = text,
            senderId = senderId,
            chatId = chatId,
            clientId = clientId,
            quotedMessageId = quotedMessage?.messageId,
            mentions = mentions,
            links = text.extractLinks(),
            fileUrl = file?.url,
            chatName = chatName,
            senderName = senderName
        )
    } else null
}

fun Message.toMessageEditRequestBody(): MessageEditRequest? {
    return if (text != null && chatId != null && senderId != null) {
        MessageEditRequest(
            text = text,
            messageId = id,
            chatId = chatId,
            senderId = senderId,
            mentions = mentions,
            links = text.extractLinks()
        )
    } else null
}

fun Message.toMessageDeleteRequestBody(): MessageDeleteRequest? {
    return if (text != null && chatId != null && senderId != null) {
        MessageDeleteRequest(
//                messageIds = listOf(id),
            messageIds = listOf(id),
            chatId = chatId,
            senderId = senderId,
        )
    } else null
}

fun Message.toQuotedMessage(): QuotedMessage {
    return QuotedMessage(
        messageId = id,
        text = text,
        senderId = senderId,
    )
}

fun ChangedMessage.composeWithMessage(message: Message): Message {
    return message.copy(
        id = id,
        chatId = chatId,
        timeUpdated = timeUpdated,
        text = text,
        senderId = senderId,
        mentions = mentions
    )
}

fun Throwable.humanMessage(resourceManager: ResourceManager): String {
    return when (this) {
        is IOException -> resourceManager.getString(R.string.connection_error)
        else -> resourceManager.getString(R.string.unknown_error)
    }
}

fun UserProfile.toContact(): Contact {
    return Contact(
        id = id,
        name = name,
        avatar = avatar,
        online = null,
        storeId = null
    )
}

fun User.toContact(): Contact {
    return Contact(
        id = id,
        name = name,
        avatar = avatar,
        online = null,
        storeId = null
    )
}

fun StoreInfo.toCreateChatRequest(
    chatName: String,
    avatarUrl: String?,
    contacts: List<Contact>
): CreateChatRequest {
    return CreateChatRequest(
        chatName = chatName,
        fileUrl = avatarUrl,
        contacts = contacts,
        storeId = storeId,
        storeName = storeName,
        partnerCode = partnerCode,
        partnerName = partnerName,
        agentCode = agentCode,
    )
}

fun View.showSoftInput() {
    if (requestFocus()) {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(this, 0)
    }
}

fun Fragment.hideSoftInput() {
    view?.let {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

fun String.extractLinks() : List<String> {
    val links = mutableListOf<String>()
    val matcher = Patterns.WEB_URL.matcher(this)
    while (matcher.find()) {
        val url = matcher.group()
        links.add(url)
    }
    return links
}

fun Context.launchCustomTab(url: String, customTabsSession: CustomTabsSession? = null) {
    val builder = customTabsSession?.let { CustomTabsIntent.Builder(it) }
        ?: CustomTabsIntent.Builder()
    val intent = builder.build()
    intent.launchUrl(this, Uri.parse(url))
}

fun Context.openFile(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Toast.makeText(
            this,
            getString(R.string.file_open_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}
