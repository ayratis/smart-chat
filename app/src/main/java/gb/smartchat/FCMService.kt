package gb.smartchat

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: $remoteMessage")
        Log.d(TAG, "onMessageReceived: data: ${remoteMessage.data}")
        ChatPushNotificationManager.proceedRemoteMessage(
            context = this,
            remoteMessage = remoteMessage,
            smartUserId = "0eeb970e-9c3f-11e2-b7e9-e41f13e6ace6" //todo
        )
    }
}
