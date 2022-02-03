package gb.smartchat.sample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import gb.smartchat.library.SmartChatActivity
import gb.smartchat.library.entity.StoreInfo

class SelectUserActivity : AppCompatActivity(R.layout.activity_select_user) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btn_user1).setOnClickListener {
            openChatList("46343a36-9ad0-4002-822d-61d81da5c831"/*"d9d39587-de82-11eb-941d-9418820bda83"*/) //Достоевский
        }
        findViewById<Button>(R.id.btn_user2).setOnClickListener {
//            openChatList("77f21ecc-0d4a-4f85-9173-55acf327f007"/*"fa736de8-dd08-11e8-80d5-ecebb89a056f"*/) //Борщев
//            openChatList("068a786d-f9f7-11eb-941d-9418820bda83") //Ayrat
            openChatList("139efc0f-5648-11e2-800f-e41f13e6ace6") // new <swnn>
        }
    }


    private fun openChatList(userId: String) {

        val storeInfoList = listOf<StoreInfo>(
//            StoreInfo(
//                "068a786e-f9f7-11eb-941d-9418820bda83",
//                "Ayrat",
//                1,
//                "Сладкая жизнь",
//                193567,
//                "http://cws.swnn.ru:4096/img/logo.png"
//            ),
//            StoreInfo(
//                "d9d39588-de82-11eb-941d-9418820bda83",
//                "Мой магазин",
//                1,
//                "Сладкая жизнь",
//                11111,
//                "http://cws.swnn.ru:4096/img/logo.png"
//            ),
//            StoreInfo(
//                "d9d39588-de82-11eb-941d-9418820bda83",
//                "Ayrat",
//                2,
//                "Smart Chef",
//                142578,
//                "http://cws.swnn.ru:4096/img/logo/logo_smart_chef.png"
//            ),
        )
        val intent = SmartChatActivity.createLaunchIntent(
            this,
            userId,
            storeInfoList,
//            baseUrl = "https://chat.swnn.ru:56479"
//            baseUrl = "http://91.201.41.157:8001/"
            baseUrl = "https://chat-dev.swnn.ru:56479"
        )
        startActivity(intent)
    }
}
