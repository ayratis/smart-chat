package gb.smartchat.library.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import gb.smartchat.BuildConfig
import gb.smartchat.library.utils.toLogsString
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class FileDownloadHelperImpl (context: Context): FileDownloadHelper {

    companion object {
        private const val TAG = "FileDownloadHelper"
    }

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val onCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Log.d(TAG, "BR onReceive: ${intent.toLogsString()}")
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        Log.d(TAG, "download completed")
                    } else {
                        downloadManager.remove(downloadId)
                    }
                }
            }
        }
    }

    init {
        context.registerReceiver(
            onCompleteListener,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun download(uri: String) : Observable<DownloadStatus> {
        if (getDownloadStatus(uri) == DownloadStatus.Empty) {
            val request = DownloadManager.Request(Uri.parse(uri)).apply {
//                setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            }
            downloadManager.enqueue(request)
        }
        return observeDownloadStatus(uri).onErrorReturnItem(DownloadStatus.Empty)
    }

    override fun cancelDownload(uri: String) {
        getDownloadId(uri)?.let { downloadManager.remove(it) }
    }

    override fun getDownloadStatus(uri: String): DownloadStatus {
        val downloadId = getDownloadId(uri) ?: return DownloadStatus.Empty
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val status: Int = cursor.getInt(
                    cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                )
                if (BuildConfig.DEBUG) {
                    val logStatus = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> "successful"
                        DownloadManager.STATUS_PENDING -> "pending"
                        DownloadManager.STATUS_PAUSED -> "paused"
                        DownloadManager.STATUS_FAILED -> "failed"
                        DownloadManager.STATUS_RUNNING -> "running"
                        else -> "unknown"
                    }
                    Log.d(TAG, "getDownloadStatus: $logStatus")
                }
                return when (status) {
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED,
                    DownloadManager.STATUS_PENDING -> {
                        val bytesDownloaded = cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )
                        val bytesTotal = cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                        Log.d(TAG, "bytesDownloaded: $bytesDownloaded")
                        Log.d(TAG, "bytesTotal: $bytesTotal")
                        val progress = bytesDownloaded.toDouble() / bytesTotal.toDouble() * 100.0
                        DownloadStatus.Downloading(progress.toInt())
                    }
                    DownloadManager.STATUS_FAILED -> DownloadStatus.Empty
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val localUri = downloadManager.getUriForDownloadedFile(downloadId)
                        DownloadStatus.Success(localUri)
                    }
                    else -> DownloadStatus.Empty
                }
            }
        }
        return DownloadStatus.Empty
    }

    private fun observeDownloadStatus(uri: String): Observable<DownloadStatus> =
        Observable.interval(200, TimeUnit.MILLISECONDS)
            .map { getDownloadStatus(uri) }
            .takeUntil { it !is DownloadStatus.Downloading }
            .observeOn(AndroidSchedulers.mainThread())

    private fun getDownloadId(uri: String): Long? {
        downloadManager.query(DownloadManager.Query()).use { cursor ->
            while (cursor.moveToNext()) {
                val cursorUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
                if (cursorUri == uri) {
                    return cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                }
            }
        }
        return null
    }
}
