package gb.smartchat.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import gb.smartchat.R
import gb.smartchat.di.Component
import gb.smartchat.ui.chat_list.ChatListFragment
import gb.smartchat.utils.configureSystemBars

class SmartChatActivity : AppCompatActivity(R.layout.layout_container) {

    val component: Component by viewModels {
        Component.Factory(
            application = application,
            userId = "77f21ecc-0d4a-4f85-9173-55acf327f007",
//            userId ="46343a36-9ad0-4002-822d-61d81da5c831",
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
