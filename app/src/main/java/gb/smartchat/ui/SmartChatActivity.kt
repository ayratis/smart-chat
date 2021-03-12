package gb.smartchat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import gb.smartchat.R
import gb.smartchat.di.Component
import gb.smartchat.ui.chat_list.ChatListFragment
import gb.smartchat.utils.configureSystemBars

class SmartChatActivity : AppCompatActivity(R.layout.layout_container) {

    companion object {
        private const val ARG_USER_ID = "arg user id"

        fun createLaunchIntent(context: Context, userId: String): Intent {
            return Intent(context, SmartChatActivity::class.java).apply {
                putExtra(ARG_USER_ID, userId)
            }
        }
    }

    private val userId: String by lazy {
        intent.getStringExtra(ARG_USER_ID)!!
    }

    val component: Component by viewModels {
        Component.Factory(
            application = application,
            userId = userId,
            baseUrl = "http://91.201.41.157:8000/"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window?.configureSystemBars()
        }
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, ChatListFragment())
                .commitNow()
        }
    }

    override fun onStart() {
        super.onStart()
        component.socket.connect()
    }
}
