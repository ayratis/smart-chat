package gb.smartchat.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import gb.smartchat.R

class SelectUserActivity : AppCompatActivity(R.layout.activity_select_user) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btn_user1).setOnClickListener {
            openChatList("46343a36-9ad0-4002-822d-61d81da5c831")
        }
        findViewById<Button>(R.id.btn_user2).setOnClickListener {
            openChatList("77f21ecc-0d4a-4f85-9173-55acf327f007")
        }
    }

    private fun openChatList(userId: String) {
        val intent = SmartChatActivity.createLaunchIntent(this, userId)
        startActivity(intent)
    }
}