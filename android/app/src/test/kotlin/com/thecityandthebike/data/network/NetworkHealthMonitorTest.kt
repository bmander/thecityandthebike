package com.thecityandthebike.data.network

import android.net.ConnectivityManager
import android.net.Network
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.HealthResponse
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkHealthMonitorTest {

    private lateinit var apiService: ApiService
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var monitor: NetworkHealthMonitor

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        apiService = mockk()
        connectivityManager = mockk(relaxed = true)
        every { connectivityManager.activeNetwork } returns mockk<Network>()
        monitor = NetworkHealthMonitor(apiService, connectivityManager, testScope)
    }

    @Test
    fun `checkHealth sets Healthy when API returns success`() = runTest {
        coEvery { apiService.health() } returns Response.success(
            HealthResponse(status = "ok", version = "1.0")
        )

        monitor.checkHealth()

        assertEquals(NetworkStatus.Healthy, monitor.status.value)
    }

    @Test
    fun `checkHealth sets ApiUnreachable when API returns error`() = runTest {
        coEvery { apiService.health() } returns Response.error(
            500,
            "".toResponseBody()
        )

        monitor.checkHealth()

        assertEquals(NetworkStatus.ApiUnreachable, monitor.status.value)
    }

    @Test
    fun `checkHealth sets ApiUnreachable on IOException`() = runTest {
        coEvery { apiService.health() } throws IOException("Connection refused")

        monitor.checkHealth()

        assertEquals(NetworkStatus.ApiUnreachable, monitor.status.value)
    }

    @Test
    fun `checkHealth sets ApiUnreachable on generic Exception`() = runTest {
        coEvery { apiService.health() } throws RuntimeException("unexpected error")

        monitor.checkHealth()

        assertEquals(NetworkStatus.ApiUnreachable, monitor.status.value)
    }

    @Test
    fun `checkHealth sets DeviceOffline when no active network`() = runTest {
        every { connectivityManager.activeNetwork } returns null

        monitor.checkHealth()

        assertEquals(NetworkStatus.DeviceOffline, monitor.status.value)
    }

    @Test
    fun `checkHealth sets Checking initially then resolves`() = runTest {
        every { connectivityManager.activeNetwork } returns mockk<Network>()
        coEvery { apiService.health() } coAnswers {
            assertEquals(NetworkStatus.Checking, monitor.status.value)
            Response.success(HealthResponse(status = "ok", version = "1.0"))
        }

        monitor.checkHealth()

        assertEquals(NetworkStatus.Healthy, monitor.status.value)
    }

    @Test
    fun `retry after failure resolves to Healthy`() = runTest {
        coEvery { apiService.health() } throws IOException("timeout")
        monitor.checkHealth()
        assertEquals(NetworkStatus.ApiUnreachable, monitor.status.value)

        coEvery { apiService.health() } returns Response.success(
            HealthResponse(status = "ok", version = "1.0")
        )
        monitor.checkHealth()
        assertEquals(NetworkStatus.Healthy, monitor.status.value)
    }

    // --- NetworkCallback behavior ---

    @Test
    fun `onNetworkLost sets DeviceOffline when no active network`() = runTest {
        every { connectivityManager.activeNetwork } returns null

        monitor.onNetworkLost()

        assertEquals(NetworkStatus.DeviceOffline, monitor.status.value)
    }

    @Test
    fun `onNetworkLost stays Healthy when another network is active`() = runTest {
        coEvery { apiService.health() } returns Response.success(
            HealthResponse(status = "ok", version = "1.0")
        )

        monitor.onNetworkLost()

        assertEquals(NetworkStatus.Healthy, monitor.status.value)
    }

    @Test
    fun `onNetworkAvailable triggers checkHealth`() = runTest {
        coEvery { apiService.health() } returns Response.success(
            HealthResponse(status = "ok", version = "1.0")
        )

        monitor.onNetworkAvailable()

        assertEquals(NetworkStatus.Healthy, monitor.status.value)
    }

    @Test
    fun `onNetworkLost then onNetworkAvailable recovers to Healthy`() = runTest {
        every { connectivityManager.activeNetwork } returns null
        monitor.onNetworkLost()
        assertEquals(NetworkStatus.DeviceOffline, monitor.status.value)

        every { connectivityManager.activeNetwork } returns mockk<Network>()
        coEvery { apiService.health() } returns Response.success(
            HealthResponse(status = "ok", version = "1.0")
        )
        monitor.onNetworkAvailable()

        assertEquals(NetworkStatus.Healthy, monitor.status.value)
    }

    @Test
    fun `initial status is Checking`() {
        assertEquals(NetworkStatus.Checking, monitor.status.value)
    }

    // --- DeviceOffline short-circuits without API call ---

    @Test
    fun `checkHealth does not call API when device is offline`() = runTest {
        every { connectivityManager.activeNetwork } returns null

        monitor.checkHealth()

        coVerify(exactly = 0) { apiService.health() }
    }
}
