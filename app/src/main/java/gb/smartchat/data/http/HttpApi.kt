package gb.smartchat.data.http

import gb.smartchat.entity.*
import gb.smartchat.entity.request.AddRecipientsRequest
import gb.smartchat.entity.request.CreateChatRequest
import gb.smartchat.entity.request.PinChatRequest
import gb.smartchat.entity.response.*
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
    ): Single<BaseResponse<List<Contact>>>

    @GET("chat/management/list")
    fun getChatList(
        @Query("page_count") pageCount: Int,
        @Query("page_size") pageSize: Int,
        @Query("from_archive") fromArchive: Boolean?
    ): Single<BaseResponse<List<Chat>>>

    @Multipart
    @POST("chat/file/upload")
    fun postUploadFile(
        @Part file: MultipartBody.Part
    ): Single<BaseResponse<File>>

    @GET("chat/contacts/list")
    @Headers("smart-user-id: 0eeb970e-9c3f-11e2-b7e9-e41f13e6ace6") //todo remove (it's test)
    fun getContactList(
        @Query("store_id") storeId: String, //GUID
        @Query("store_name") storeName: String,
        @Query("partner_code") partnerCode: Int,
        @Query("partner_name") partnerName: String,
        @Query("agent_code") agentCode: Int
    ): Single<BaseResponse<ContactListResponse>>

    @POST("chat/management/create")
    fun postCreateChat(
        @Body createChatRequest: CreateChatRequest
    ): Single<BaseResponse<Chat>>

    @GET("chat/contacts/profile")
    fun getUserProfile(
        @Query("another_user_id") anotherUserId: String? = null
    ): Single<BaseResponse<UserProfile>>

    @GET("chat/management/search")
    fun getSearchChats(
        @Query("search_string") query: String, //min length 3
        @Query("page_count") pageCount: Int,
        @Query("page_size") pageSize: Int
    ): Single<BaseResponse<ChatSearchResponse>>

    @POST("chat/management/pin")
    fun postPinChat(
        @Body request: PinChatRequest
    ): Single<BaseResponse<PinChatResponse>>

    @POST("chat/management/unpin")
    fun postUnpinChat(
        @Body request: PinChatRequest
    ): Single<BaseResponse<PinChatResponse>>

    @POST("chat/management/archive")
    fun postArchiveChat(
        @Body request: PinChatRequest
    ): Single<BaseResponse<PinChatResponse>>

    @POST("chat/management/unarchive")
    fun postUnarchiveChat(
        @Body request: PinChatRequest
    ): Single<BaseResponse<PinChatResponse>>

    @GET("chat/file/files")
    fun getFiles(
        @Query("chat_id") chatId: Long,
        @Query("user_id") userId: String? = null,
        @Query("type") type: String? = null,  //media|regular
        @Query("page_count") pageCount: Int,
        @Query("page_size") pageSize: Int
    ): Single<BaseResponse<List<File>>>

    @GET("chat/management/links")
    fun getLinks(
        @Query("chat_id") chatId: Long,
        @Query("user_id") userId: String? = null,
        @Query("page_count") pageCount: Int,
        @Query("page_size") pageSize: Int
    ): Single<BaseResponse<LinksResponse>>

    @POST("chat/management/add_recipients")
    fun postAddRecipients(
        @Body request: AddRecipientsRequest
    ): Single<BaseResponse<Any>>

    @POST("chat/management/delete_recipients")
    fun postDeleteRecipients(
        @Body request: AddRecipientsRequest
    ): Single<BaseResponse<Any>>
}
