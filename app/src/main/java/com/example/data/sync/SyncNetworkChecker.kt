package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

data class SyncNetworkState(
    val isConnected: Boolean = false,
    val isUnmeteredWifi: Boolean = false
)

interface SyncNetworkChecker {
    fun currentState(): SyncNetworkState
    fun observeChanges(onNetworkAvailable: () -> Unit): () -> Unit = { }
}

class AndroidSyncNetworkChecker(
    private val context: Context
) : SyncNetworkChecker {

    override fun currentState(): SyncNetworkState {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return SyncNetworkState(isConnected = false, isUnmeteredWifi = false)
            val activeNetwork = cm.activeNetwork ?: return SyncNetworkState(false, false)
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return SyncNetworkState(false, false)
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            SyncNetworkState(isConnected = hasInternet, isUnmeteredWifi = isWifi)
        } catch (_: Exception) {
            SyncNetworkState(isConnected = false, isUnmeteredWifi = false)
        }
    }

    override fun observeChanges(onNetworkAvailable: () -> Unit): () -> Unit {
        val cm = try {
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        } catch (_: Exception) {
            null
        } ?: return {}

        // registerDefaultNetworkCallback yêu cầu API 24+; minSdk 23 nên trả về no-op trên API 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return {}

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkAvailable()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    onNetworkAvailable()
                }
            }
        }
        return try {
            cm.registerDefaultNetworkCallback(callback)
            val unregister: () -> Unit = {
                try {
                    cm.unregisterNetworkCallback(callback)
                } catch (_: Exception) {
                }
            }
            unregister
        } catch (_: Exception) {
            {}
        }
    }
}
