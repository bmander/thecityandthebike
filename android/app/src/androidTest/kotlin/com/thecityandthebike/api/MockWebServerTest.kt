package com.thecityandthebike.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class MockWebServerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val client = OkHttpClient.Builder().build()
        val contentType = "application/json".toMediaType()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun login_success_returnsToken() = runBlocking {
        val responseJson = """
            {
                "access_token": "test_jwt_token",
                "refresh_token": "test_refresh_token",
                "token_type": "bearer"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.login(LoginRequest("testuser", "testpass"))

        assertTrue(response.isSuccessful)
        assertEquals("test_jwt_token", response.body()?.accessToken)
        assertEquals("bearer", response.body()?.tokenType)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/login", request.path)
    }

    @Test
    fun login_invalidCredentials_returns401() = runBlocking {
        val responseJson = """
            {
                "detail": {"msg": "Bad username or password"}
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.login(LoginRequest("wronguser", "wrongpass"))

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun register_success_returnsMessage() = runBlocking {
        val responseJson = """
            {
                "msg": "User created"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.register(
            RegisterRequest("newuser", "password123")
        )

        assertTrue(response.isSuccessful)
        assertEquals("User created", response.body()?.msg)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/register", request.path)
    }

    @Test
    fun register_userExists_returns409() = runBlocking {
        val responseJson = """
            {
                "detail": {"msg": "User with that username or email already exists"}
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.register(
            RegisterRequest("existinguser", "password123")
        )

        assertFalse(response.isSuccessful)
        assertEquals(409, response.code())
    }

    @Test
    fun getSubmissions_success_returnsListOfSubmissions() = runBlocking {
        val responseJson = """
            {
                "items": [
                    {
                        "submission_id": "sub1",
                        "user_id": "user1",
                        "bike_qr_id": "bike1",
                        "image_url": "http://example.com/image1.jpg",
                        "captured_date": "2024-01-15",
                        "uploaded_at": "2024-01-15T10:31:00Z",
                        "user_caption": "Nice bike!"
                    },
                    {
                        "submission_id": "sub2",
                        "user_id": "user1",
                        "bike_qr_id": "bike2",
                        "image_url": "http://example.com/image2.jpg",
                        "captured_date": "2024-01-16",
                        "uploaded_at": "2024-01-16T14:01:00Z",
                        "user_caption": null
                    }
                ],
                "total": 2,
                "limit": 20,
                "offset": 0
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getSubmissions()

        assertTrue(response.isSuccessful)
        val submissions = response.body()?.items
        assertNotNull(submissions)
        assertEquals(2, submissions?.size)
        assertEquals("sub1", submissions?.get(0)?.submissionId)
        assertEquals("bike1", submissions?.get(0)?.bikeQrId)
        assertEquals("2024-01-15", submissions?.get(0)?.capturedDate)
    }

    @Test
    fun getSubmissions_unauthorized_returns401() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"detail": {"msg": "Not authenticated"}}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getSubmissions()

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }
}
