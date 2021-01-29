package gb.smartchat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import gb.smartchat.R
import gb.smartchat.utils.configureSystemBars

class AppActivity : AppCompatActivity(R.layout.layout_container) {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window?.configureSystemBars()
        }
        super.onCreate(savedInstanceState)
    }
}