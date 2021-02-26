package gb.smartchat.data.http

import gb.smartchat.entity.Message
import gb.smartchat.entity.Recipient
import gb.smartchat.entity.response.BaseResponse
import gb.smartchat.entity.response.FileUploadResponse
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*

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

    @Multipart
    @POST("chat/file/upload")
    fun postUploadFile(
        @Part file: MultipartBody.Part
    ): Single<BaseResponse<FileUploadResponse>>
}
