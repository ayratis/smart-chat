package gb.smartchat.data.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable

@Suppress("DEPRECATION")
class ConnectionManagerImpl(context: Context) : ConnectionManager {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val isOnlineRelay = BehaviorRelay.createDefault(false)

    override val isOnline: Observable<Boolean> = isOnlineRelay.hide().distinctUntilChanged()

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val networkRequest = NetworkRequest.Builder().build()
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isOnlineRelay.accept(true)
                }

                override fun onLost(network: Network) {
                    isOnlineRelay.accept(false)
                }
            }
            cm.registerNetworkCallback(networkRequest, networkCallback)
        } else {
            val connectivityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val isOnline = cm.activeNetworkInfo?.isAvailable == true
                    isOnlineRelay.accept(isOnline)
                }
            }
            context.registerReceiver(
                connectivityReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
    }
}