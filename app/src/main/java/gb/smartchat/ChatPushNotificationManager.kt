package gb.smartchat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import gb.smartchat.entity.Message

object ChatPushNotificationManager {

    const val CHANNEL_ID = "smart_chat_channel"
    const val ARG_CHAT_ID = "arg chat id"
    const val ACTION_PUSH = "action push"
    const val CHANNEL_NAME = "smart-chat-channel"

    fun proceedRemoteMessage(
        context: Context,
        remoteMessage: RemoteMessage,
        @DrawableRes iconRes: Int = 0,
        smartUserId: String
    ): Boolean {
        if (remoteMessage.data["is_chat_push_message"] != "true") return false
        val senderId = remoteMessage.data["sender_id"] ?: return false
        val chatId = remoteMessage.data["chat_id"] ?: return false
        val title = remoteMessage.data["title"] ?: return false
        val message = remoteMessage.data["message"] ?: return false

        val chatIdLong = try {
            chatId.toLong()
        } catch (e: Throwable) {
            return false
        }

        sendPush(
            context,
            chatIdLong,
            title,
            message,
            smartUserId,
            iconRes,
        )

        return true
    }

    fun proceedSocketMessage(context: Context, message: Message, smartUserId: String) {
        message.chatId ?: return
        val title = message.chatId.toString()
        val text = message.text ?: ""

        sendPush(
            context,
            message.chatId,
            title,
            text,
            smartUserId,
            iconRes = 0,
        )
    }

    private fun sendPush(
        context: Context,
        chatId: Long,
        title: String,
        message: String,
        smartUserId: String,
        @DrawableRes iconRes: Int = 0,
    ) {
        createNotificationChannel(context)

        val intent = SmartChatActivity.createLaunchIntent(context, smartUserId, chatId).apply {
            action = ACTION_PUSH
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            kotlin.random.Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            if (iconRes != 0) {
                setSmallIcon(iconRes)
            } else {
                val applicationInfo = context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
                val appIconResId = applicationInfo.icon
                setSmallIcon(appIconResId)
            }
            setContentTitle(title)
            setContentText(message)
            setAutoCancel(true)
            priority = NotificationCompat.PRIORITY_MAX
            setContentIntent(pendingIntent)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(kotlin.random.Random.nextInt(), builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            // Register the channel with the system
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
