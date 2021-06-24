package gb.smartchat.sample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import gb.smartchat.library.SmartChatActivity

class SelectUserActivity : AppCompatActivity(R.layout.activity_select_user) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btn_user1).setOnClickListener {
            openChatList(/*"46343a36-9ad0-4002-822d-61d81da5c831"*/"0eeb970e-9c3f-11e2-b7e9-e41f13e6ace6") //Достоевский
        }
        findViewById<Button>(R.id.btn_user2).setOnClickListener {
            openChatList(/*"77f21ecc-0d4a-4f85-9173-55acf327f007"*/"fa736de8-dd08-11e8-80d5-ecebb89a056f") //Борщев
        }
    }

    private fun openChatList(userId: String) {
        val intent = SmartChatActivity.createLaunchIntent(this, userId, emptyList())
        startActivity(intent)
    }
}
