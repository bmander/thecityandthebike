package com.thecityandthebike.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsernameValidationTest {

    @Test
    fun `empty username returns null`() {
        assertNull(usernameError(""))
    }

    @Test
    fun `valid username returns null`() {
        assertNull(usernameError("testuser"))
        assertNull(usernameError("abc"))
        assertNull(usernameError("user_123"))
    }

    @Test
    fun `username too short returns error`() {
        assertEquals("Username must be at least 3 characters", usernameError("ab"))
        assertEquals("Username must be at least 3 characters", usernameError("a"))
    }

    @Test
    fun `username at minimum length is valid`() {
        assertNull(usernameError("abc"))
    }

    @Test
    fun `username too long returns error`() {
        val longName = "a".repeat(51)
        assertEquals("Username must be 50 characters or fewer", usernameError(longName))
    }

    @Test
    fun `username at maximum length is valid`() {
        assertNull(usernameError("a".repeat(50)))
    }

    @Test
    fun `username with spaces returns error`() {
        assertEquals("Only letters, numbers, and underscores allowed", usernameError("test user"))
    }

    @Test
    fun `username with dash returns error`() {
        assertEquals("Only letters, numbers, and underscores allowed", usernameError("test-user"))
    }

    @Test
    fun `username with dot returns error`() {
        assertEquals("Only letters, numbers, and underscores allowed", usernameError("test.user"))
    }

    @Test
    fun `username with at sign returns error`() {
        assertEquals("Only letters, numbers, and underscores allowed", usernameError("test@user"))
    }

    @Test
    fun `username with underscore is valid`() {
        assertNull(usernameError("test_user"))
    }

    @Test
    fun `username with digits is valid`() {
        assertNull(usernameError("user123"))
    }

    @Test
    fun `username with mixed case is valid`() {
        assertNull(usernameError("TestUser"))
    }
}
