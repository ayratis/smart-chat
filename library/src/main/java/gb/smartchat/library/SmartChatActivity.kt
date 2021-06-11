package gb.smartchat.library

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import gb.smartchat.R
import gb.smartchat.library.entity.Message
import gb.smartchat.library.ui.chat.ChatFragment
import gb.smartchat.library.ui.chat_list.ChatListFragment
import gb.smartchat.library.utils.*
import io.reactivex.disposables.CompositeDisposable

class SmartChatActivity : AppCompatActivity(R.layout.layout_container) {

    companion object {
        private const val TAG = "SmartChatActivity"
        private const val ARG_USER_ID = "arg user id"
        private const val ARG_CHAT_ID_TO_OPEN = "arg chat id to open"

        fun createLaunchIntent(context: Context, userId: String, chatId: Long = 0): Intent {
            return Intent(context, SmartChatActivity::class.java).apply {
                putExtra(ARG_USER_ID, userId)
                putExtra(ARG_CHAT_ID_TO_OPEN, chatId)
            }
        }
    }

    private val compositeDisposable = CompositeDisposable()
    private val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.fragment_container)

    private val userId: String by lazy {
        intent.getStringExtra(ARG_USER_ID)!!
    }

    private val argChatIdToOpen: Long by lazy {
        intent.getLongExtra(ARG_CHAT_ID_TO_OPEN, 0)
    }

    val component: Component by viewModels {
        Component.Factory(
            application = application,
            userId = userId,
            baseUrl = "http://91.201.41.157:8001/"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window?.configureSystemBars()
        }
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {

            //если перешли по пушу
            if (argChatIdToOpen != 0L) {
                supportFragmentManager.newRootChain(
                    ChatListFragment.create(false),
                    ChatFragment.create(chatId = argChatIdToOpen, chat = null),
                )
                return
            }

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, ChatListFragment.create(false))
                .commitNow()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        Log.d(TAG, "onNewIntent: ${intent.toLogsString()}")

        if (intent.action == ChatPushNotificationManager.ACTION_PUSH) {
            val chatId: Long = intent.getLongExtra(ARG_CHAT_ID_TO_OPEN, 0L)
            if (chatId == 0L) return
            supportFragmentManager.newScreenFromRoot(
                ChatFragment.create(chatId = chatId, chat = null),
                NavAnim.SLIDE
            )
        }
    }

    override fun onStart() {
        super.onStart()
        component.connect()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        component.sendNewMessagePush
            .subscribe(this::proceedNewMessage)
            .also { compositeDisposable.add(it) }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun proceedNewMessage(message: Message) {
        val currentFragment = currentFragment

        if (currentFragment is ChatListFragment) {
            return
        }

        if (currentFragment is ChatFragment) {
            if (!currentFragment.isSameChat(message)) {
                ChatPushNotificationManager.proceedSocketMessage(this, message, userId)
            }
            return
        }

        ChatPushNotificationManager.proceedSocketMessage(this, message, userId)
    }
}
