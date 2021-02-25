package gb.smartchat.data.http

import gb.smartchat.entity.Message
import gb.smartchat.entity.Recipient
import gb.smartchat.entity.response.BaseResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface HttpApi {

    @GET("chat/management/messages/history")
    fun getChatMessageHistory(
        @Query("chat_id") chatId: Long,
        @Query("page_size") pageSize: Int,
        @Query("message_id") messageId: Long?,
        @Query("look_forward") lookForward: Boolean
    ): Single<BaseResponse<List<Message>>>

    @GET("chat/management/recipients")
    fun getRecipients(
        @Query("chat_id") chatId: Long
    ): Single<BaseResponse<List<Recipient>>>
}