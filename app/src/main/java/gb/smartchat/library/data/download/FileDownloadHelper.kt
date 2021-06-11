package gb.smartchat.library.data.download

import io.reactivex.Observable

interface FileDownloadHelper {
    fun getDownloadStatus(uri: String): DownloadStatus
    fun download(uri: String): Observable<DownloadStatus>
    fun cancelDownload(uri: String)
}
