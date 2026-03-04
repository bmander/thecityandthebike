package com.thecityandthebike.data.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NetworkStatus {
    data object Healthy : NetworkStatus
    data object Checking : NetworkStatus
    data object DeviceOffline : NetworkStatus
    data object ApiUnreachable : NetworkStatus
}

interface NetworkStatusProvider {
    val status: StateFlow<NetworkStatus>
    suspend fun checkHealth()
}

@Singleton
class NetworkHealthMonitor @Inject constructor(
    private val apiService: ApiService,
    private val connectivityManager: ConnectivityManager,
    @ApplicationScope private val appScope: CoroutineScope
) : NetworkStatusProvider {
    private val _status = MutableStateFlow<NetworkStatus>(NetworkStatus.Checking)
    override val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private val monitoring = AtomicBoolean(false)
    private val checking = AtomicBoolean(false)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onNetworkAvailable()
        }

        override fun onLost(network: Network) {
            onNetworkLost()
        }
    }

    fun startMonitoring() {
        if (!monitoring.compareAndSet(false, true)) return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        appScope.launch { checkHealth() }
    }

    internal fun onNetworkAvailable() {
        appScope.launch { checkHealth() }
    }

    internal fun onNetworkLost() {
        appScope.launch { checkHealth() }
    }

    override suspend fun checkHealth() {
        if (!checking.compareAndSet(false, true)) return
        try {
            _status.value = NetworkStatus.Checking
            if (connectivityManager.activeNetwork == null) {
                _status.value = NetworkStatus.DeviceOffline
                return
            }
            try {
                val response = apiService.health()
                _status.value = if (response.isSuccessful) NetworkStatus.Healthy else NetworkStatus.ApiUnreachable
            } catch (_: IOException) {
                _status.value = NetworkStatus.ApiUnreachable
            } catch (_: Exception) {
                _status.value = NetworkStatus.ApiUnreachable
            }
        } finally {
            checking.set(false)
        }
    }
}
