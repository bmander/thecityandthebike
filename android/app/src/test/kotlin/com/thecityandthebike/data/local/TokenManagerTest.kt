package com.thecityandthebike.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TokenManagerTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockContext: Context
    private lateinit var tokenManager: TokenManager
    private lateinit var store: MutableMap<String, String?>

    @Before
    fun setup() {
        store = mutableMapOf()

        mockEditor = mockk<SharedPreferences.Editor>()
        every { mockEditor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg()
            mockEditor
        }
        every { mockEditor.remove(any()) } answers {
            store.remove(firstArg<String>())
            mockEditor
        }
        every { mockEditor.apply() } just Runs

        mockPrefs = mockk<SharedPreferences>()
        every { mockPrefs.edit() } returns mockEditor
        every { mockPrefs.getString(any(), isNull()) } answers { store[firstArg()] }

        mockkConstructor(MasterKey.Builder::class)
        every { anyConstructed<MasterKey.Builder>().setKeyScheme(any()) } answers {
            self as MasterKey.Builder
        }
        every { anyConstructed<MasterKey.Builder>().build() } returns mockk<MasterKey>()

        mockkStatic(EncryptedSharedPreferences::class)
        every {
            EncryptedSharedPreferences.create(
                any<Context>(), any<String>(), any<MasterKey>(), any(), any()
            )
        } returns mockPrefs

        mockContext = mockk<Context>(relaxed = true)
        tokenManager = TokenManager(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `saveTokens and getToken and getRefreshToken round-trip`() {
        tokenManager.saveTokens("access123", "refresh456")

        assertEquals("access123", tokenManager.getToken())
        assertEquals("refresh456", tokenManager.getRefreshToken())
    }

    @Test
    fun `clearToken removes both tokens and sets isLoggedIn to false`() {
        tokenManager.saveTokens("access", "refresh")
        assertTrue(tokenManager.isLoggedIn.value)

        tokenManager.clearToken()

        assertNull(tokenManager.getToken())
        assertNull(tokenManager.getRefreshToken())
        assertFalse(tokenManager.isLoggedIn.value)
    }

    @Test
    fun `hasToken returns true when token exists`() {
        store["auth_token"] = "some_token"
        assertTrue(tokenManager.hasToken())
    }

    @Test
    fun `hasToken returns false when token is null`() {
        assertFalse(tokenManager.hasToken())
    }

    @Test
    fun `hasToken returns false when token is empty`() {
        store["auth_token"] = ""
        assertFalse(tokenManager.hasToken())
    }

    @Test
    fun `getUserId correctly decodes JWT sub claim`() {
        mockkStatic(Base64::class)
        val payload = """{"sub":"user123"}"""
        val encodedPayload = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray())
        store["auth_token"] = "header.$encodedPayload.signature"

        every { Base64.decode(any<String>(), any<Int>()) } answers {
            java.util.Base64.getUrlDecoder().decode(firstArg<String>())
        }

        assertEquals("user123", tokenManager.getUserId())
    }

    @Test
    fun `getUserId returns null when no token`() {
        assertNull(tokenManager.getUserId())
    }

    @Test
    fun `getUserId returns null for malformed token without three parts`() {
        store["auth_token"] = "not-a-jwt"
        assertNull(tokenManager.getUserId())
    }

    @Test
    fun `getUserId returns null for invalid base64 payload`() {
        mockkStatic(Base64::class)
        store["auth_token"] = "header.!!!invalid!!!.signature"

        every { Base64.decode(any<String>(), any<Int>()) } throws IllegalArgumentException("Bad base64")

        assertNull(tokenManager.getUserId())
    }

    @Test
    fun `getUserId returns null when payload has no sub claim`() {
        mockkStatic(Base64::class)
        val payload = """{"name":"alice"}"""
        val encodedPayload = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray())
        store["auth_token"] = "header.$encodedPayload.signature"

        every { Base64.decode(any<String>(), any<Int>()) } answers {
            java.util.Base64.getUrlDecoder().decode(firstArg<String>())
        }

        assertNull(tokenManager.getUserId())
    }

    @Test
    fun `isLoggedIn emits true after save and false after clear`() = runTest {
        tokenManager.isLoggedIn.test {
            assertEquals(false, awaitItem())

            tokenManager.saveTokens("token", "refresh")
            assertEquals(true, awaitItem())

            tokenManager.clearToken()
            assertEquals(false, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isAdmin returns true when JWT has admin true`() {
        mockkStatic(Base64::class)
        val payload = """{"sub":"user123","admin":true}"""
        val encodedPayload = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray())
        store["auth_token"] = "header.$encodedPayload.signature"

        every { Base64.decode(any<String>(), any<Int>()) } answers {
            java.util.Base64.getUrlDecoder().decode(firstArg<String>())
        }

        assertTrue(tokenManager.isAdmin())
    }

    @Test
    fun `isAdmin returns false when JWT has admin false`() {
        mockkStatic(Base64::class)
        val payload = """{"sub":"user123","admin":false}"""
        val encodedPayload = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray())
        store["auth_token"] = "header.$encodedPayload.signature"

        every { Base64.decode(any<String>(), any<Int>()) } answers {
            java.util.Base64.getUrlDecoder().decode(firstArg<String>())
        }

        assertFalse(tokenManager.isAdmin())
    }

    @Test
    fun `isAdmin returns false when JWT has no admin claim`() {
        mockkStatic(Base64::class)
        val payload = """{"sub":"user123"}"""
        val encodedPayload = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray())
        store["auth_token"] = "header.$encodedPayload.signature"

        every { Base64.decode(any<String>(), any<Int>()) } answers {
            java.util.Base64.getUrlDecoder().decode(firstArg<String>())
        }

        assertFalse(tokenManager.isAdmin())
    }

    @Test
    fun `isAdmin returns false when no token`() {
        assertFalse(tokenManager.isAdmin())
    }

    @Test
    fun `isAdmin returns false for malformed token`() {
        store["auth_token"] = "not-a-jwt"
        assertFalse(tokenManager.isAdmin())
    }

    @Test
    fun `constructor recovers from corrupted encrypted preferences`() {
        // Clean up default setup mocks
        unmockkAll()

        val freshStore = mutableMapOf<String, String?>()

        val freshEditor = mockk<SharedPreferences.Editor>()
        every { freshEditor.putString(any(), any()) } answers {
            freshStore[firstArg()] = secondArg()
            freshEditor
        }
        every { freshEditor.remove(any()) } answers {
            freshStore.remove(firstArg<String>())
            freshEditor
        }
        every { freshEditor.apply() } just Runs

        val goodPrefs = mockk<SharedPreferences>()
        every { goodPrefs.edit() } returns freshEditor
        every { goodPrefs.getString(any(), isNull()) } answers { freshStore[firstArg()] }

        mockkConstructor(MasterKey.Builder::class)
        every { anyConstructed<MasterKey.Builder>().setKeyScheme(any()) } answers {
            self as MasterKey.Builder
        }
        every { anyConstructed<MasterKey.Builder>().build() } returns mockk<MasterKey>()

        mockkStatic(EncryptedSharedPreferences::class)
        var createCallCount = 0
        every {
            EncryptedSharedPreferences.create(
                any<Context>(), any<String>(), any<MasterKey>(), any(), any()
            )
        } answers {
            createCallCount++
            if (createCallCount == 1) throw RuntimeException("Corrupted!")
            goodPrefs
        }

        val clearEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { clearEditor.clear() } returns clearEditor

        val corruptPrefs = mockk<SharedPreferences>()
        every { corruptPrefs.edit() } returns clearEditor

        val context = mockk<Context>(relaxed = true)
        every { context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE) } returns corruptPrefs

        // Act: constructor should recover from corruption
        val tm = TokenManager(context)

        // Verify corruption was detected and cleared
        verify { clearEditor.clear() }
        verify { clearEditor.apply() }
        assertEquals(2, createCallCount)
    }
}
