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
import gb.smartchat.library.entity.StoreInfo
import gb.smartchat.library.ui._global.MessageDialogFragment
import gb.smartchat.library.ui.chat.ChatFragment
import gb.smartchat.library.ui.chat_list.ChatListFragment
import gb.smartchat.library.ui.username_missing.UsernameMissingFragment
import gb.smartchat.library.utils.*
import io.reactivex.disposables.CompositeDisposable

class SmartChatActivity : AppCompatActivity(R.layout.layout_container),
    MessageDialogFragment.OnClickListener {

    companion object {
        private const val TAG = "SmartChatActivity"
        private const val ARG_USER_ID = "arg user id"
        private const val ARG_CHAT_ID_TO_OPEN = "arg chat id to open"
        private const val ARG_STORE_INFO_LIST = "arg store info list"
        private const val USER_MISSING_TAG = "user missing tag"

        fun createLaunchIntent(
            context: Context,
            userId: String,
            storeInfoList: List<StoreInfo>,
            chatId: Long = 0
        ): Intent {
            return Intent(context, SmartChatActivity::class.java).apply {
                putExtra(ARG_USER_ID, userId)
                putExtra(ARG_STORE_INFO_LIST, ArrayList(storeInfoList))
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

    private val storeInfoList: List<StoreInfo> by lazy {
        val serializable = intent.getSerializableExtra(ARG_STORE_INFO_LIST)
        @Suppress("UNCHECKED_CAST")
        (serializable as ArrayList<StoreInfo>).toList()
    }

    private val argChatIdToOpen: Long by lazy {
        intent.getLongExtra(ARG_CHAT_ID_TO_OPEN, 0)
    }

    val component: Component by viewModels {
        Component.Factory(
            application = application,
            userId = userId,
            baseUrl = "http://91.201.41.157:8001/",
            storeInfoList = storeInfoList
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

        component.userMissing
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    supportFragmentManager.newRootScreen(
                        UsernameMissingFragment(),
                        NavAnim.OPEN
                    )
                }
            }
            .also { compositeDisposable.add(it) }

        component.usernameMissing
            .subscribe { event ->
                event.getContentIfNotHandled()?.let {
                    MessageDialogFragment
                        .create(
                            message = getString(R.string.user_missing_message),
                            tag = USER_MISSING_TAG
                        )
                        .show(supportFragmentManager, USER_MISSING_TAG)
                }
            }
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
                ChatPushNotificationManager.proceedSocketMessage(
                    this,
                    userId,
                    storeInfoList,
                    message
                )
            }
            return
        }

        ChatPushNotificationManager.proceedSocketMessage(this, userId, storeInfoList, message)
    }

    override fun dialogCanceled(tag: String) {
        when (tag) {
            USER_MISSING_TAG -> finish()
        }
    }

    override fun dialogPositiveClicked(tag: String) {
        when (tag) {
            USER_MISSING_TAG -> finish()
        }
    }
}
