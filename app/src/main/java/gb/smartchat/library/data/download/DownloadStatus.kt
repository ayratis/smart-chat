package gb.smartchat.library.data.download

import android.net.Uri

sealed class DownloadStatus {
    object Empty : DownloadStatus()
    data class Downloading(val progress: Int) : DownloadStatus()
    data class Success(val contentUri: Uri) : DownloadStatus()
}
