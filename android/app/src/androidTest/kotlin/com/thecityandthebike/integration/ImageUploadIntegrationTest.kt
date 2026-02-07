package com.thecityandthebike.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.SubmissionCreate
import com.thecityandthebike.integration.IntegrationTestUtils.copyAssetToFile
import com.thecityandthebike.integration.IntegrationTestUtils.createApiService
import com.thecityandthebike.integration.IntegrationTestUtils.createAuthenticatedApiService
import com.thecityandthebike.integration.IntegrationTestUtils.createImageMultipart
import com.thecityandthebike.integration.IntegrationTestUtils.registerAndLogin
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ImageUploadIntegrationTest {

    private lateinit var apiService: ApiService
    private lateinit var testImageFile: File

    @Before
    fun setup() {
        apiService = createApiService()
        val context = InstrumentationRegistry.getInstrumentation().context
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        testImageFile = copyAssetToFile(context, "test_image.jpg", targetContext.cacheDir)
    }

    @Test
    fun uploadImage_withValidAuth_succeeds() = runBlocking {
        // Register and login to get a valid token
        val loginResult = registerAndLogin(apiService)
        assertTrue("Login should succeed", loginResult is IntegrationTestUtils.LoginResult.Success)

        val successResult = loginResult as IntegrationTestUtils.LoginResult.Success
        val authenticatedService = createAuthenticatedApiService(successResult.token)

        // Upload image
        val imagePart = createImageMultipart(testImageFile)
        val response = authenticatedService.uploadImage(imagePart)

        assertTrue("Image upload should succeed", response.isSuccessful)
        assertEquals(201, response.code())

        val uploadResponse = response.body()
        assertNotNull("Response body should not be null", uploadResponse)
        assertNotNull("URL should not be null", uploadResponse?.url)
        assertNotNull("Filename should not be null", uploadResponse?.filename)
        assertTrue("URL should contain /uploads/images/", uploadResponse?.url?.contains("/uploads/images/") == true)
        assertTrue("Filename should end with .jpg", uploadResponse?.filename?.endsWith(".jpg") == true)
    }

    @Test
    fun uploadImage_withoutAuth_fails() = runBlocking {
        // Try to upload image without authentication
        val imagePart = createImageMultipart(testImageFile)
        val response = apiService.uploadImage(imagePart)

        assertFalse("Image upload without auth should fail", response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun uploadImage_thenCreateSubmission_succeeds() = runBlocking {
        // Register and login to get a valid token
        val loginResult = registerAndLogin(apiService)
        assertTrue("Login should succeed", loginResult is IntegrationTestUtils.LoginResult.Success)

        val successResult = loginResult as IntegrationTestUtils.LoginResult.Success
        val authenticatedService = createAuthenticatedApiService(successResult.token)

        // Upload image
        val imagePart = createImageMultipart(testImageFile)
        val uploadResponse = authenticatedService.uploadImage(imagePart)
        assertTrue("Image upload should succeed", uploadResponse.isSuccessful)

        val uploadResult = uploadResponse.body()
        assertNotNull("Upload response should not be null", uploadResult)

        // Create submission with uploaded image URL
        val submission = SubmissionCreate(
            bikeQrId = "BIKE_${UUID.randomUUID().toString().take(8)}",
            imageUrlOriginal = uploadResult!!.url,
            imageUrlProcessed = uploadResult.url,
            capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString(),
            userCaption = "Test submission with uploaded image"
        )

        val submissionResponse = authenticatedService.createSubmission(submission)
        assertTrue("Create submission should succeed", submissionResponse.isSuccessful)
        assertEquals(201, submissionResponse.code())

        val createdSubmission = submissionResponse.body()
        assertNotNull("Created submission should not be null", createdSubmission)
        assertEquals(uploadResult.url, createdSubmission?.imageUrlOriginal)
    }

    @Test
    fun uploadImage_withInvalidExtension_fails() = runBlocking {
        // Register and login to get a valid token
        val loginResult = registerAndLogin(apiService)
        assertTrue("Login should succeed", loginResult is IntegrationTestUtils.LoginResult.Success)

        val successResult = loginResult as IntegrationTestUtils.LoginResult.Success
        val authenticatedService = createAuthenticatedApiService(successResult.token)

        // Create a temp file with .txt extension
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val textFile = File.createTempFile("test", ".txt", targetContext.cacheDir)
        textFile.writeText("This is not an image")

        // Try to upload text file as image
        val requestFile = textFile.asRequestBody("text/plain".toMediaTypeOrNull())
        val textPart = MultipartBody.Part.createFormData("image", textFile.name, requestFile)
        val response = authenticatedService.uploadImage(textPart)

        assertFalse("Upload with invalid extension should fail", response.isSuccessful)
        assertEquals(400, response.code())

        // Clean up
        textFile.delete()
        Unit
    }
}
