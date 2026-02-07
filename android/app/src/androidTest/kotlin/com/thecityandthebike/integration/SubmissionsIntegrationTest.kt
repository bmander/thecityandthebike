package com.thecityandthebike.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.SubmissionCreate
import com.thecityandthebike.integration.IntegrationTestUtils.createApiService
import com.thecityandthebike.integration.IntegrationTestUtils.createAuthenticatedApiService
import com.thecityandthebike.integration.IntegrationTestUtils.registerAndLogin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SubmissionsIntegrationTest {

    private lateinit var apiService: ApiService

    @Before
    fun setup() {
        apiService = createApiService()
    }

    @Test
    fun getSubmissions_withValidToken_succeeds() = runBlocking {
        // Register and login to get a valid token
        val loginResult = registerAndLogin(apiService)
        assertTrue("Login should succeed", loginResult is IntegrationTestUtils.LoginResult.Success)

        val successResult = loginResult as IntegrationTestUtils.LoginResult.Success
        val authenticatedService = createAuthenticatedApiService(successResult.token)

        // Get submissions with authenticated service
        val response = authenticatedService.getSubmissions()
        assertTrue("Get submissions should succeed", response.isSuccessful)
        assertEquals(200, response.code())
        assertNotNull("Response body should not be null", response.body())
    }

    @Test
    fun getSubmissions_withoutToken_succeeds() = runBlocking {
        // Get submissions without authentication (public endpoint)
        val response = apiService.getSubmissions()
        assertTrue("Get submissions without token should succeed (public endpoint)", response.isSuccessful)
        assertEquals(200, response.code())
        assertNotNull("Response body should not be null", response.body())
    }

    @Test
    fun getMySubmissions_withValidToken_succeeds() = runBlocking {
        // Register and login to get a valid token
        val loginResult = registerAndLogin(apiService)
        assertTrue("Login should succeed", loginResult is IntegrationTestUtils.LoginResult.Success)

        val successResult = loginResult as IntegrationTestUtils.LoginResult.Success
        val authenticatedService = createAuthenticatedApiService(successResult.token)

        // Get user's submissions with authenticated service
        val response = authenticatedService.getMySubmissions()
        assertTrue("Get my submissions should succeed", response.isSuccessful)
        assertEquals(200, response.code())
        assertNotNull("Response body should not be null", response.body())
    }

    @Test
    fun createSubmission_withValidData_succeeds() = runBlocking {
        // Register and login to get a valid token
        val loginResult = registerAndLogin(apiService)
        assertTrue("Login should succeed", loginResult is IntegrationTestUtils.LoginResult.Success)

        val successResult = loginResult as IntegrationTestUtils.LoginResult.Success
        val authenticatedService = createAuthenticatedApiService(successResult.token)

        // Create a submission
        val submission = SubmissionCreate(
            bikeQrId = "BIKE_${UUID.randomUUID().toString().take(8)}",
            imageUrlOriginal = "https://example.com/test_image_original.jpg",
            imageUrlProcessed = "https://example.com/test_image_processed.jpg",
            capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString(),
            userCaption = "Test submission"
        )

        val response = authenticatedService.createSubmission(submission)
        assertTrue("Create submission should succeed", response.isSuccessful)
        assertEquals(201, response.code())

        val createdSubmission = response.body()
        assertNotNull("Response body should not be null", createdSubmission)
        assertEquals(submission.bikeQrId, createdSubmission?.bikeQrId)
        assertEquals(submission.userCaption, createdSubmission?.userCaption)
    }

    @Test
    fun createSubmission_withoutToken_fails() = runBlocking {
        // Try to create a submission without authentication
        val submission = SubmissionCreate(
            bikeQrId = "BIKE_${UUID.randomUUID().toString().take(8)}",
            imageUrlOriginal = "https://example.com/test_image_original.jpg",
            imageUrlProcessed = "https://example.com/test_image_processed.jpg",
            capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString()
        )

        val response = apiService.createSubmission(submission)
        assertFalse("Create submission without token should fail", response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun createSubmission_thenGetMySubmissions_containsCreatedSubmission() = runBlocking {
        // Register and login to get a valid token
        val loginResult = registerAndLogin(apiService)
        assertTrue("Login should succeed", loginResult is IntegrationTestUtils.LoginResult.Success)

        val successResult = loginResult as IntegrationTestUtils.LoginResult.Success
        val authenticatedService = createAuthenticatedApiService(successResult.token)

        // Create a submission
        val bikeQrId = "BIKE_${UUID.randomUUID().toString().take(8)}"
        val submission = SubmissionCreate(
            bikeQrId = bikeQrId,
            imageUrlOriginal = "https://example.com/test_image_original.jpg",
            imageUrlProcessed = "https://example.com/test_image_processed.jpg",
            capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString()
        )

        val createResponse = authenticatedService.createSubmission(submission)
        assertTrue("Create submission should succeed", createResponse.isSuccessful)

        // Verify submission appears in my submissions
        val mySubmissionsResponse = authenticatedService.getMySubmissions()
        assertTrue("Get my submissions should succeed", mySubmissionsResponse.isSuccessful)

        val submissions = mySubmissionsResponse.body()
        assertNotNull("Response body should not be null", submissions)
        assertTrue(
            "My submissions should contain the created submission",
            submissions?.any { it.bikeQrId == bikeQrId } == true
        )
    }
}
