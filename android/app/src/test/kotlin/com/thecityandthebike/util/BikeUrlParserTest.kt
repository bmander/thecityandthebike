package com.thecityandthebike.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeUrlParserTest {

    @Test
    fun `whitelisted providers contains expected values`() {
        assertEquals(setOf("lime", "bird"), WHITELISTED_PROVIDERS)
    }

    @Test
    fun `lime url strips padding`() {
        val result = parseBikeUrl("https://lime.bike/bc/v1/G5EZAYI=")
        assertEquals("lime", result.provider)
        assertEquals("G5EZAYI", result.bikeId)
    }

    @Test
    fun `lime url strips multiple padding`() {
        val result = parseBikeUrl("https://lime.bike/bc/v1/ABC==")
        assertEquals("lime", result.provider)
        assertEquals("ABC", result.bikeId)
    }

    @Test
    fun `lime url without padding`() {
        val result = parseBikeUrl("https://lime.bike/bc/v1/G5EZAYI")
        assertEquals("lime", result.provider)
        assertEquals("G5EZAYI", result.bikeId)
    }

    @Test
    fun `bird url`() {
        val result = parseBikeUrl("https://ride.bird.co/bc/v1/abc123")
        assertEquals("bird", result.provider)
        assertEquals("abc123", result.bikeId)
    }

    @Test
    fun `unknown url returns null provider`() {
        val result = parseBikeUrl("https://unknown-provider.com/bikes/XYZ")
        assertNull(result.provider)
        assertEquals("https://unknown-provider.com/bikes/XYZ", result.bikeId)
    }

    @Test
    fun `plain string returns null provider`() {
        val result = parseBikeUrl("BIKE-001")
        assertNull(result.provider)
        assertEquals("BIKE-001", result.bikeId)
    }

    @Test
    fun `empty string returns null provider`() {
        val result = parseBikeUrl("")
        assertNull(result.provider)
        assertEquals("", result.bikeId)
    }

    @Test
    fun `url with no path returns null provider`() {
        val result = parseBikeUrl("https://lime.bike/")
        assertNull(result.provider)
        assertEquals("https://lime.bike/", result.bikeId)
    }

    @Test
    fun `http lime url works`() {
        val result = parseBikeUrl("http://lime.bike/bc/v1/G5EZAYI=")
        assertEquals("lime", result.provider)
        assertEquals("G5EZAYI", result.bikeId)
    }

    @Test
    fun `isRecognizedProvider returns true for lime`() {
        assertTrue(isRecognizedProvider("https://lime.bike/bc/v1/G5EZAYI="))
    }

    @Test
    fun `isRecognizedProvider returns true for bird`() {
        assertTrue(isRecognizedProvider("https://ride.bird.co/bc/v1/abc123"))
    }

    @Test
    fun `isRecognizedProvider returns false for unknown`() {
        assertFalse(isRecognizedProvider("https://unknown.com/bikes/XYZ"))
    }

    @Test
    fun `isRecognizedProvider returns false for plain string`() {
        assertFalse(isRecognizedProvider("BIKE-001"))
    }
}
