package gb.smartchat.library.data.http

import gb.smartchat.library.entity.*
import gb.smartchat.library.entity.request.*
import gb.smartchat.library.entity.response.*
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
    fun getContactList(
        @Query("store_id") storeId: String?, //GUID
    ): Single<BaseResponse<ContactListResponse>>

    @POST("chat/management/create")
    fun postCreateChat(
        @Body createChatRequest: CreateChatRequest
    ): Single<BaseResponse<CreateChatResponse>>

    @GET("chat/contacts/profile")
    fun getUserProfile(
        @Query("another_user_id") anotherUserId: String? = null
    ): Single<BaseResponse<UserProfile>>

    @GET("chat/management/search")
    fun getSearchChats(
        @Query("search_string") query: String, //min length 3
        @Query("page_count") pageCount: Int,
        @Query("page_size") pageSize: Int,
        @Query("with_contacts") withContacts: Boolean
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

    @Multipart
    @POST("chat/management/avatar")
    fun postUploadChatAvatar(
        @Part file: MultipartBody.Part
    ): Single<BaseResponse<File>>

    @Multipart
    @POST("chat/contacts/avatar")
    fun postUploadUserAvatar(
        @Part file: MultipartBody.Part
    ): Single<BaseResponse<File>>

    @POST("chat/management/leave")
    fun postLeaveChat(
        @Body request: AddRecipientsRequest
    ): Single<BaseResponse<Any>>

    @POST("chat/management/messages/favorite")
    fun postMessageFavorite(
        @Body request: MessageFavoriteRequest
    ): Single<BaseResponse<Boolean>>

    @GET("chat/management/get_favorite_chat")
    fun getFavoriteChat(): Single<BaseResponse<FavoriteChatResponse>>

    @GET("chat/management/get")
    fun getChat(
        @Query("chat_id") chatId: Long
    ): Single<BaseResponse<ChatResponse>>

    @POST("chat/contacts/add_username")
    fun postUsername(
        @Body request: UsernameRequest
    ): Single<BaseResponse<Any>>

    @GET("chat/management/get_service_chat")
    fun getServiceChat(
        @Query("store_id") storeId: String?, //GUID
        @Query("partner_code") partnerCode: Int?,
    ): Single<BaseResponse<ChatResponse>>
}
