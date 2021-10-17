package gb.smartchat.library.data.http

data class ServerException(
    val httpCode: Int,
    override val message: String?
) : Exception(message)
