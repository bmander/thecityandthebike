package com.thecityandthebike.util

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class ImageUrlUtilTest {

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `full http URL should be parsed directly`() {
        val url = "http://example.com/image.jpg"
        imageUrlToUri(url)
        verify { Uri.parse(url) }
    }

    @Test
    fun `full https URL should be parsed directly`() {
        val url = "https://example.com/images/photo.png"
        imageUrlToUri(url)
        verify { Uri.parse(url) }
    }

    @Test
    fun `relative URL should get BASE_URL prepended`() {
        val relativeUrl = "/uploads/image.jpg"
        imageUrlToUri(relativeUrl)
        verify { Uri.parse(com.thecityandthebike.BuildConfig.BASE_URL + relativeUrl) }
    }

    @Test
    fun `relative URL without leading slash should get BASE_URL prepended`() {
        val relativeUrl = "uploads/image.jpg"
        imageUrlToUri(relativeUrl)
        verify { Uri.parse(com.thecityandthebike.BuildConfig.BASE_URL + relativeUrl) }
    }
}
