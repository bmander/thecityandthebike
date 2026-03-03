package com.thecityandthebike.ui.screens

import com.thecityandthebike.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsernameValidationTest {

    @Test
    fun `empty username returns null`() {
        assertNull(usernameErrorRes(""))
    }

    @Test
    fun `valid username returns null`() {
        assertNull(usernameErrorRes("testuser"))
        assertNull(usernameErrorRes("abc"))
        assertNull(usernameErrorRes("user_123"))
    }

    @Test
    fun `username too short returns error`() {
        assertEquals(R.string.username_error_too_short, usernameErrorRes("ab"))
        assertEquals(R.string.username_error_too_short, usernameErrorRes("a"))
    }

    @Test
    fun `username at minimum length is valid`() {
        assertNull(usernameErrorRes("abc"))
    }

    @Test
    fun `username too long returns error`() {
        val longName = "a".repeat(51)
        assertEquals(R.string.username_error_too_long, usernameErrorRes(longName))
    }

    @Test
    fun `username at maximum length is valid`() {
        assertNull(usernameErrorRes("a".repeat(50)))
    }

    @Test
    fun `username with spaces returns error`() {
        assertEquals(R.string.username_error_invalid_chars, usernameErrorRes("test user"))
    }

    @Test
    fun `username with dash returns error`() {
        assertEquals(R.string.username_error_invalid_chars, usernameErrorRes("test-user"))
    }

    @Test
    fun `username with dot returns error`() {
        assertEquals(R.string.username_error_invalid_chars, usernameErrorRes("test.user"))
    }

    @Test
    fun `username with at sign returns error`() {
        assertEquals(R.string.username_error_invalid_chars, usernameErrorRes("test@user"))
    }

    @Test
    fun `username with underscore is valid`() {
        assertNull(usernameErrorRes("test_user"))
    }

    @Test
    fun `username with digits is valid`() {
        assertNull(usernameErrorRes("user123"))
    }

    @Test
    fun `username with mixed case is valid`() {
        assertNull(usernameErrorRes("TestUser"))
    }

    @Test
    fun `username with trailing space returns error`() {
        assertEquals(R.string.username_error_invalid_chars, usernameErrorRes("testuser "))
    }

    @Test
    fun `username with leading space returns error`() {
        assertEquals(R.string.username_error_invalid_chars, usernameErrorRes(" testuser"))
    }
}
