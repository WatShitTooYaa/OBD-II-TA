package com.example.obd_iiservice.internet

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class NetworkStatus {
    Available, Unavailable, Losing, Lost
}

class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                launch { send(NetworkStatus.Available) } // Gunakan launch untuk aman dari thread utama
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                launch { send(NetworkStatus.Losing) }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                launch { send(NetworkStatus.Lost) }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                launch { send(NetworkStatus.Unavailable) }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // --- INI BAGIAN PENTINGNYA ---
        // 1. Cek status jaringan saat ini secara manual.
        val currentNetwork = connectivityManager.activeNetwork
        if (currentNetwork == null) {
            // Jika tidak ada jaringan aktif sama sekali.
            launch { send(NetworkStatus.Unavailable) }
        } else {
            // Cek apakah jaringan yang aktif punya kapabilitas internet.
            val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
            if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                launch { send(NetworkStatus.Available) }
            } else {
                launch { send(NetworkStatus.Unavailable) }
            }
        }
        // --- AKHIR BAGIAN PENTING ---

        // unregister callback saat flow di-cancel.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() // Mencegah emit nilai yang sama berturut-turut.
}