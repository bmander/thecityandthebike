package com.thecityandthebike.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderColorTest {

    private val fallback = Color(0xFFFF0000)

    @Test
    fun `lime provider returns green`() {
        assertEquals(Color(0xFF4CAF50), providerColor("lime", fallback))
    }

    @Test
    fun `bird provider returns blue`() {
        assertEquals(Color(0xFF2196F3), providerColor("bird", fallback))
    }

    @Test
    fun `lime is case insensitive`() {
        assertEquals(Color(0xFF4CAF50), providerColor("Lime", fallback))
        assertEquals(Color(0xFF4CAF50), providerColor("LIME", fallback))
    }

    @Test
    fun `bird is case insensitive`() {
        assertEquals(Color(0xFF2196F3), providerColor("Bird", fallback))
        assertEquals(Color(0xFF2196F3), providerColor("BIRD", fallback))
    }

    @Test
    fun `unknown provider returns fallback`() {
        assertEquals(fallback, providerColor("citi", fallback))
        assertEquals(fallback, providerColor("lyft", fallback))
    }
}
