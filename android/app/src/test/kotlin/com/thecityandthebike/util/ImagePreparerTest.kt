package com.thecityandthebike.util

import android.content.Context
import android.net.Uri
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ImagePreparerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var imagePreparer: ImagePreparer

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk()
        mockkStatic("com.thecityandthebike.util.FileUtilsKt")
        imagePreparer = ImagePreparer(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("com.thecityandthebike.util.FileUtilsKt")
    }

    @Test
    fun `prepareImageFile should return file for valid URI`() = runTest {
        val uri = mockk<Uri>()
        val file = mockk<File>(relaxed = true)

        every { uriToFile(context, uri) } returns file
        every { stripMetadata(file) } returns Unit

        val result = imagePreparer.prepareImageFile(uri)

        assertEquals(file, result)
    }

    @Test
    fun `prepareImageFile should return null when uriToFile returns null`() = runTest {
        val uri = mockk<Uri>()

        every { uriToFile(context, uri) } returns null

        val result = imagePreparer.prepareImageFile(uri)

        assertNull(result)
    }

    @Test
    fun `prepareImageFile should call stripMetadata on valid file`() = runTest {
        val uri = mockk<Uri>()
        val file = mockk<File>(relaxed = true)

        every { uriToFile(context, uri) } returns file
        every { stripMetadata(file) } returns Unit

        imagePreparer.prepareImageFile(uri)

        coVerify { stripMetadata(file) }
    }

    @Test
    fun `prepareImageFile should return file even when stripMetadata throws`() = runTest {
        val uri = mockk<Uri>()
        val file = mockk<File>(relaxed = true)

        every { uriToFile(context, uri) } returns file
        every { stripMetadata(file) } throws RuntimeException("EXIF error")

        val result = imagePreparer.prepareImageFile(uri)

        assertEquals(file, result)
    }
}
