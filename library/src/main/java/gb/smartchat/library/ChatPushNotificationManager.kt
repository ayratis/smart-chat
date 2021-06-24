package gb.smartchat.library

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import gb.smartchat.library.entity.Message
import gb.smartchat.library.entity.StoreInfo

object ChatPushNotificationManager {

    const val CHANNEL_ID = "smart_chat_channel"
    const val ARG_CHAT_ID = "arg chat id"
    const val ACTION_PUSH = "action push"
    const val CHANNEL_NAME = "smart-chat-channel"

    fun proceedRemoteMessage(
        context: Context,
        smartUserId: String,
        storeInfoList: List<StoreInfo>,
        dataMap: Map<String, String>,
        @DrawableRes iconRes: Int = 0,
    ): Boolean {
        if (dataMap["is_chat_push_message"] != "true") return false
        val chatId = dataMap["chat_id"] ?: return false
        val title = dataMap["title"] ?: return false
        val message = dataMap["message"] ?: return false

        val chatIdLong = try {
            chatId.toLong()
        } catch (e: Throwable) {
            return false
        }

        sendPush(
            context,
            smartUserId,
            storeInfoList,
            chatIdLong,
            title,
            message,
            iconRes,
        )

        return true
    }

    fun proceedSocketMessage(
        context: Context,
        smartUserId: String,
        storeInfoList: List<StoreInfo>,
        message: Message
    ) {
        message.chatId ?: return
        val title = message.chatName ?: ""
        val text = with(StringBuilder()) {
            message.senderName?.let(this::append)
            if (length > 0) append(": ")
            message.text?.let(this::append)
            toString()
        }

        sendPush(
            context,
            smartUserId,
            storeInfoList,
            message.chatId,
            title,
            text,
            iconRes = 0,
        )
    }

    private fun sendPush(
        context: Context,
        smartUserId: String,
        storeInfoList: List<StoreInfo>,
        chatId: Long,
        title: String,
        message: String,
        @DrawableRes iconRes: Int = 0,
    ) {
        createNotificationChannel(context)

        val intent = SmartChatActivity.createLaunchIntent(
            context,
            smartUserId,
            storeInfoList,
            chatId
        ).apply {
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
