package gb.smartchat.library

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import gb.smartchat.BuildConfig
import gb.smartchat.library.entity.Message
import gb.smartchat.library.entity.StoreInfo
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException

object ChatPushNotificationManager {

    const val CHANNEL_ID = "smart_chat_channel"
    const val ARG_CHAT_ID = "arg chat id"
    const val ACTION_PUSH = "action push"
    const val CHANNEL_NAME = "smart-chat-channel"

    /**
     * @param deviceType - ios | android | huawei | no_google
     */
    fun registerPushToken(
        smartUserId: String,
        token: String,
        deviceType: String,
        baseUrl: String = "https://chat.swnn.ru:56479"
    ) {
        val bodyJson = JSONObject().apply {
            put("token", token)
            put("device_type", deviceType)
        }
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            bodyJson.toString()
        )
        val request = Request.Builder()
            .url("$baseUrl/chat/contacts/put_notifications_token")
            .method("POST", body)
            .header("smart-user-id", smartUserId)
            .build()

        val okHttpClientBuilder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            okHttpClientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
        val okHttpClient = okHttpClientBuilder.build()

        okHttpClient.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("PushNotificationManager", "onRegisterTokenFailure: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(
                        "PushNotificationManager",
                        "onRegisterTokenResponse: " +
                                "isSuccessful: ${response.isSuccessful}" +
                                "code: ${response.code()}" +
                                "message: ${response.message()}"
                    )
                }
            })
    }

    fun proceedRemoteMessage(
        context: Context,
        launchMode: LaunchMode = LaunchMode.ChatList,
        smartUserId: String,
        storeInfoList: List<StoreInfo>,
        dataMap: Map<String, String>,
        @DrawableRes iconRes: Int = 0,
        baseUrl: String = "https://chat.swnn.ru:56479"
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
            launchMode,
            smartUserId,
            storeInfoList,
            chatIdLong,
            title,
            message,
            iconRes,
            baseUrl
        )

        return true
    }

    fun proceedSocketMessage(
        context: Context,
        launchMode: LaunchMode,
        smartUserId: String,
        storeInfoList: List<StoreInfo>,
        message: Message,
        baseUrl: String = "https://chat.swnn.ru:56479"
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
            launchMode,
            smartUserId,
            storeInfoList,
            message.chatId,
            title,
            text,
            iconRes = 0,
            baseUrl
        )
    }

    private fun sendPush(
        context: Context,
        launchMode: LaunchMode,
        smartUserId: String,
        storeInfoList: List<StoreInfo>,
        chatId: Long,
        title: String,
        message: String,
        @DrawableRes iconRes: Int = 0,
        baseUrl: String
    ) {
        createNotificationChannel(context)

        val intent = SmartChatActivity.createLaunchIntent(
            context,
            launchMode,
            smartUserId,
            storeInfoList,
            chatId,
            baseUrl
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
