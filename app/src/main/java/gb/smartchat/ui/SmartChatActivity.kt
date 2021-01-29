package gb.smartchat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import gb.smartchat.R
import gb.smartchat.ui.chat.ChatFragment
import gb.smartchat.utils.configureSystemBars

class SmartChatActivity : AppCompatActivity(R.layout.layout_container) {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window?.configureSystemBars()
        }
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChatFragment())
                .commitNow()
        }
    }
}