package gb.smartchat.data.http

import gb.smartchat.entity.Message
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface HttpApi {

    @GET("chat/management/messages/history")
    fun getChatMessageHistory(
        @Query("chat_id") chatId: Long,
        @Query("page_size") pageSize: Int,
        @Query("message_id") messageId: Long,
        @Query("look_forward") lookForward: Boolean
    ): Single<List<Message>>
}